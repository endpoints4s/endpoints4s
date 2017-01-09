package cqrs

import akka.actor.ActorSystem
import akka.stream.{Materializer, ActorMaterializer}
import play.api.{Application, BuiltInComponents, Configuration, Environment, Mode}
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.ws.{WSAPI, WSClient, WSClientConfig}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.api.routing.Router
import play.core.server.{NettyServer, ServerConfig}
import play.core.{ApplicationProvider, DefaultWebCommands, SourceMapper, WebCommands}
import scala.concurrent.Future
import com.typesafe.config.ConfigFactory

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

  // Start the commands service
  val commandsServer =
    HttpServer(ServerConfig(port = Some(commandsService.port)), Router.from(Commands.routes))

  // Start the queries service
  val queriesServer =
    HttpServer(ServerConfig(port = Some(queriesService.port)), Router.from(Queries.routes))

  // Start the public server
  def baseUrl(port: Int): String = s"http://localhost:$port"
  implicit val actorSystem: ActorSystem = ActorSystem("public-service")
  implicit val materializer: Materializer = ActorMaterializer()
  val wsClient = AhcWSClient(AhcWSClientConfig())
  val publicServer =
    new PublicServer(baseUrl(commandsService.port), baseUrl(queriesService.port), wsClient)
  // …

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      wsClient.close()
      commandsServer.stop()
      queriesServer.stop()
      // publicServer.stop()
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
