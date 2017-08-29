package cqrs.publicserver.documented

import java.net.URLEncoder
import java.util.UUID

import _root_.play.api.libs.ws.WSClient
import _root_.play.api.routing.{Router => PlayRouter}
import cats.Traverse
import cqrs.commands.{AddRecord, CreateMeter, MeterCreated, StoredEvent}
import cqrs.publicserver.{CommandsClient, QueriesClient}
import cqrs.queries._
import cats.instances.option._
import cats.instances.future._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

//#delegate-interpreter
import endpoints.documented.delegate
import endpoints.play

//#delegate-interpreter
/**
  * Implementation of the public API based on our “commands” and “queries” microservices.
  *
  * This version is similar to [[cqrs.publicserver.PublicServer]], but uses delegation
  * to apply the [[play.server]] interpreters to an [[endpoints.documented.algebra]] algebra.
  */
//#delegate-interpreter
class PublicServer(
  commandsBaseUrl: String,
  queriesBaseUrl: String,
  wsClient: WSClient)(implicit
  ec: ExecutionContext
) extends PublicEndpoints
  with delegate.Endpoints
  with delegate.CirceJsonSchemaEntities
  with delegate.OptionalResponses {

  lazy val delegate =
    new play.server.Endpoints
      with play.server.CirceEntities
      with play.server.OptionalResponses

  import delegate.{circeJsonDecoder, circeJsonEncoder}

//#delegate-interpreter
  private val commandsClient = new CommandsClient(commandsBaseUrl, wsClient)

  private val queriesClient = new QueriesClient(queriesBaseUrl, wsClient)

  val routes: PlayRouter.Routes =
    delegate.routesFromEndpoints(

      listMeters.implementedByAsync { _ =>
        //#invocation
        val metersList: Future[ResourceList] = queriesClient.query(FindAll)
        //#invocation
        metersList.map(_.value)
      },

      getMeter.implementedByAsync { id =>
        queriesClient.query(FindById(id, None)).map(_.value)
      },

      createMeter.implementedByAsync { createData =>
        for {
          maybeEvent <- commandsClient.command(CreateMeter(createData.label))
          maybeMeter <- Traverse[Option].flatSequence(
            maybeEvent.collect {
              case StoredEvent(t, MeterCreated(id, _)) =>
                //#invocation-find-by-id
                val maybeMeter: Future[MaybeResource] = queriesClient.query(FindById(id, after = Some(t)))
                //#invocation-find-by-id
                maybeMeter.map(_.value)
            }
          )
          meter <- maybeMeter.fold[Future[Meter]](Future.failed(new NoSuchElementException))(Future.successful)
        } yield meter
      },

      addRecord.implementedByAsync { case (id, addData) =>
        for {
          maybeEvent <- commandsClient.command(AddRecord(id, addData.date, addData.value))
          findMeter = (evt: StoredEvent) => queriesClient.query(FindById(id, after = Some(evt.timestamp))).map(_.value)
          maybeMeter <- Traverse[Option].flatTraverse(maybeEvent)(findMeter)
          meter <- maybeMeter.fold[Future[Meter]](Future.failed(new NoSuchElementException))(Future.successful)
        } yield meter
      }

    )

  //#segment-uuid
  implicit lazy val uuidSegment: Segment[UUID] =
    new Segment[UUID] {
      def decode(segment: String): Option[UUID] = Try(UUID.fromString(segment)).toOption
      def encode(uuid: UUID): String = URLEncoder.encode(uuid.toString, delegate.utf8Name)
    }
  //#segment-uuid

  // These aliases are probably due to a limitation of circe
  implicit private def circeEncoderReq: io.circe.Encoder[QueryReq] = QueryReq.queryEncoder
  implicit private def circeDecoderResp: io.circe.Decoder[QueryResp] = QueryResp.queryDecoder

}
