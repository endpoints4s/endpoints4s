package cqrs.infra

import cqrs.commands.Commands
import cqrs.publicserver.{BootstrapEndpoints, PublicServer}
import cqrs.queries.{Queries, QueriesService}
import endpoints4s.play.server.PlayComponents
import play.api.Mode
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.api.routing.Router
import play.core.server.{DefaultNettyServerComponents, ServerConfig}

/** In the real world we would run the different services on distinct
  * machines.
  *
  * But, because this example is just … an example, we want to be able to run
  * everything with a single `sbt run` invocation. So, we run all the
  * services (including serving the web client assets) within the same JVM.
  */
object Main extends App {

  object commandsService extends PlayService(port = 9001, Mode.Prod) {
    lazy val commands = new Commands(playComponents)
    lazy val router = Router.from(commands.routes)
  }

  object queriesService extends PlayService(port = 9002, Mode.Prod) {
    lazy val wsClient = AhcWSClient(AhcWSClientConfig())
    lazy val service = new QueriesService(
      baseUrl(commandsService.port),
      wsClient,
      actorSystem.scheduler
    )
    lazy val queries = new Queries(service, playComponents)
    lazy val router = Router.from(queries.routes)
  }

  object publicService extends PlayService(port = 9000, Mode.Prod) {
    lazy val routes =
      new cqrs.publicserver.Router(
        new PublicServer(
          baseUrl(commandsService.port),
          baseUrl(queriesService.port),
          queriesService.wsClient,
          playComponents
        ),
        new BootstrapEndpoints(playComponents)
      ).routes
    lazy val router = Router.from(routes)
  }

  def baseUrl(port: Int): String = s"http://localhost:$port"

  // Start the commands service
  commandsService.server
  // Start the queries service
  queriesService.server
  // Start the public server
  publicService.server

  // …

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      queriesService.wsClient.close()
      commandsService.server.stop()
      queriesService.server.stop()
      publicService.server.stop()
    }
  })

}

abstract class PlayService(val port: Int, mode: Mode) extends DefaultNettyServerComponents {
  override lazy val serverConfig = ServerConfig(port = Some(port), mode = mode)
  lazy val playComponents = PlayComponents.fromBuiltInComponents(this)
}
