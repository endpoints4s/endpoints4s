package cqrs

import java.net.URLEncoder
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.{AsyncFreeSpec, BeforeAndAfterAll}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.core.server.ServerConfig
import play.api.routing.Router
import cqrs.commands.Commands
import cqrs.queries.{Queries, QueriesService}
import cqrs.publicserver.{PublicEndpoints, PublicServer}
import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.infra.HttpServer
import endpoints.play.client.{CirceEntities, Endpoints, OptionalResponses}

class Test extends AsyncFreeSpec with BeforeAndAfterAll {

  val commandsPort = 9000
  val queriesPort = 9001
  val publicPort = 9002
  def baseUrl(port: Int): String = s"http://localhost:$port"

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()
  private val wsClient = AhcWSClient(AhcWSClientConfig())

  val commandsServer =
    HttpServer(ServerConfig(port = Some(commandsPort)), Router.from(Commands.routes))

  val queriesServer = {
    val service = new QueriesService(baseUrl(commandsPort), wsClient, actorSystem.scheduler)
    val queries = new Queries(service)
    HttpServer(ServerConfig(port = Some(queriesPort)), Router.from(queries.routes))
  }



  private val publicService = new PublicServer(baseUrl(commandsPort), baseUrl(queriesPort), wsClient)
  private val publicServer = HttpServer(ServerConfig(port = Some(publicPort)), Router.from(publicService.routes))

  override def afterAll(): Unit = {
    publicServer.stop()
    wsClient.close()
    queriesServer.stop()
    commandsServer.stop()
  }

  object api
    extends Endpoints(baseUrl(publicPort), wsClient)
      with CirceEntities
      with OptionalResponses
      with PublicEndpoints {

    def uuidSegment: Segment[UUID] =
      new Segment[UUID] {
        def encode(uuid: UUID): String = URLEncoder.encode(uuid.toString, utf8Name)
      }

  }

  "Public server" - {

    "create a new meter and query it" in {
      for {
        meter     <- api.createMeter(CreateMeter("Electricity"))
        allMeters <- api.listMeters(())
      } yield assert(allMeters contains meter)
    }

  }

}
