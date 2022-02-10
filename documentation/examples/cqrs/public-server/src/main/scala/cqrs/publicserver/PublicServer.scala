package cqrs.publicserver

import cats.Traverse
import cats.effect.{IO, Resource}
import cats.implicits._
import cqrs.queries._
import cqrs.commands.{AddRecord, CreateMeter, MeterCreated, StoredEvent}
import endpoints4s.http4s.server
import org.http4s.Uri
import org.http4s.client.{Client => Http4sClient}

/** Implementation of the public API based on our “commands” and “queries” microservices.
  */
class PublicServer(
    commandsAuthority: Uri.Authority,
    queriesAuthority: Uri.Authority,
    http4sClient: Http4sClient[IO]
) extends server.Endpoints[IO]
    with server.JsonEntitiesFromCodecs
    with PublicEndpoints {

  private val commandsClient =
    new CommandsClient(commandsAuthority, Uri.Scheme.http, http4sClient)
  //#invocation
  private val queriesClient =
    new QueriesClient(queriesAuthority, Uri.Scheme.http, http4sClient)
  //#invocation

  val routes =
    routesFromEndpoints(
      listMeters.implementedByEffect { _ =>
        //#invocation
        val metersList: Resource[IO, ResourceList] = queriesClient.query.send(FindAll)
        //#invocation
        metersList.use(resourceList => IO.pure(resourceList.value))
      },
      getMeter.implementedByEffect { id =>
        queriesClient.query
          .send(FindById(id, None))
          .use(maybeResource => IO.pure(maybeResource.value))
      },
      createMeter.implementedByEffect { createData =>
        //#microservice-endpoint-invocation
        val eventuallyMaybeEvent: IO[Option[StoredEvent]] =
          commandsClient.command.sendAndConsume(CreateMeter(createData.label))
        //#microservice-endpoint-invocation
        for {
          maybeEvent <- eventuallyMaybeEvent
          maybeMeter <- Traverse[Option].flatSequence(
            maybeEvent.collect { case StoredEvent(t, MeterCreated(id, _)) =>
              //#invocation-find-by-id
              val maybeMeter: IO[MaybeResource] =
                queriesClient.query.send(FindById(id, after = Some(t))).use(IO.pure)
              //#invocation-find-by-id
              maybeMeter.map(_.value)
            }
          )
          meter <- maybeMeter.fold[IO[Meter]](
            IO.raiseError(new NoSuchElementException)
          )(IO.pure)
        } yield meter
      },
      addRecord.implementedByEffect { case (id, addData) =>
        for {
          maybeEvent <- commandsClient.command.sendAndConsume(
            AddRecord(id, addData.date, addData.value)
          )
          findMeter =
            (evt: StoredEvent) =>
              queriesClient.query
                .send(FindById(id, after = Some(evt.timestamp)))
                .use(IO.pure)
                .map(_.value)
          maybeMeter <- Traverse[Option].flatTraverse(maybeEvent)(findMeter)
          meter <- maybeMeter.fold[IO[Meter]](
            IO.raiseError(new NoSuchElementException)
          )(IO.pure)
        } yield meter
      }
    )

  // These aliases are probably due to a limitation of circe
  implicit private def circeEncoderReq: io.circe.Encoder[QueryReq] =
    QueryReq.queryEncoder
  implicit private def circeDecoderResp: io.circe.Decoder[QueryResp] =
    QueryResp.queryDecoder

}
