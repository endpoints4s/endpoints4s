package cqrs.infra

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import play.api.{BuiltInComponents, Configuration, Environment}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.api.routing.Router
import play.core.server.{NettyServer, ServerConfig}
import play.core.{ApplicationProvider, DefaultWebCommands, SourceMapper, WebCommands}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory
import cqrs.publicserver.PublicServer
import cqrs.commands.Commands
import cqrs.queries.{Queries, QueriesService}

/**
  * In the real world we would run the different services on distinct
  * machines.
  *
  * But, because this example is just … an example, we want to be able to run
  * everything with a single `sbt run` invocation. So, we run all the
  * services (including serving the web client assets) within the same JVM.
  */
object Main extends App {

  object publicService {
    val port = 9000
  }

  object commandsService {
    val port = 9001
  }

  object queriesService {
    val port = 9002
  }

  def baseUrl(port: Int): String = s"http://localhost:$port"

  implicit val actorSystem: ActorSystem = ActorSystem("public-service")
  implicit val materializer: Materializer = ActorMaterializer()
  val wsClient = AhcWSClient(AhcWSClientConfig())

  // Start the commands service
  //#start-server
  val commandsServer =
    HttpServer(ServerConfig(port = Some(commandsService.port)), Router.from(Commands.routes))
  //#start-server

  // Start the queries service
  val queriesServer = {
    val service = new QueriesService(baseUrl(commandsService.port), wsClient, actorSystem.scheduler)
    val queries = new Queries(service)
    HttpServer(ServerConfig(port = Some(queriesService.port)), Router.from(queries.routes))
  }

  // Start the public server
  val publicServer = {
    val routes =
      new cqrs.publicserver.Router(
        new PublicServer(baseUrl(commandsService.port), baseUrl(queriesService.port), wsClient)
      ).routes
    HttpServer(ServerConfig(port = Some(publicService.port)), Router.from(routes))
  }

  // …

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      wsClient.close()
      commandsServer.stop()
      queriesServer.stop()
      publicServer.stop()
    }
  })

}

object HttpServer {

  def apply(config: ServerConfig, _router: Router): NettyServer = new BuiltInComponents {
    val router: Router = _router
    lazy val environment: Environment = Environment.simple(mode = config.mode)
    lazy val sourceMapper: Option[SourceMapper] = None
    lazy val webCommands: WebCommands = new DefaultWebCommands
    lazy val configuration: Configuration = Configuration(ConfigFactory.load())

    def serverStopHook(): Future[_] = Future.successful(())

    val server =
      new NettyServer(config, ApplicationProvider(application), serverStopHook _, actorSystem)
  }.server

}
