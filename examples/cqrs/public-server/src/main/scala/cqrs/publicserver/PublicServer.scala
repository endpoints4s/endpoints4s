package cqrs.publicserver

import cats.Traverse
import cqrs.commands.{AddRecord, CreateMeter, MeterCreated}
import cqrs.queries.{FindAll, FindById, QueryReq, QueryResp}
import play.api.libs.ws.WSClient
import play.api.routing.{Router => PlayRouter}
import cats.instances.option._
import cats.instances.future._
import endpoints.play.routing.{CirceEntities, Endpoints, OptionalResponses}

import scala.concurrent.{ExecutionContext, Future}

class PublicServer(
  commandsBaseUrl: String,
  queriesBaseUrl: String,
  wsClient: WSClient)(implicit
  ec: ExecutionContext
) extends PublicEndpoints
  with Endpoints
  with CirceEntities
  with OptionalResponses {

  private val commandsClient = new CommandsClient(commandsBaseUrl, wsClient)
  private val queriesClient = new QueriesClient(queriesBaseUrl, wsClient)

  val routes: PlayRouter.Routes =
    routesFromEndpoints(
      createMeter.implementedByAsync { createData =>
        for {
          maybeEvent <- commandsClient.command(CreateMeter(createData.label))
          maybeMeter <- Traverse[Option].flatSequence(
            maybeEvent.collect {
              case MeterCreated(id, _) =>
                queriesClient.query(FindById(id))(circeJsonEncoder(QueryReq.queryEncoder), circeJsonDecoder(QueryResp.queryDecoder)).map(_.value)
            }
          )
        } yield maybeMeter
      },
      addRecord.implementedByAsync { addData =>
        commandsClient.command(AddRecord(addData.id, addData.date, addData.value))
          .flatMap(_.fold[Future[Unit]](Future.failed(new NoSuchElementException))(_ => Future.successful(())))
      },
      listMeters.implementedByAsync { _ =>
        queriesClient.query(FindAll)(circeJsonEncoder(QueryReq.queryEncoder), circeJsonDecoder(QueryResp.queryDecoder)).map { results =>
          results.value
        }
      }
    )

}
