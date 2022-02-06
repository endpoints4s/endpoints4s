package cqrs.infra

import cats.effect.IO
import cqrs.commands.Commands
import cqrs.publicserver.{BootstrapEndpoints, PublicServer}
import cqrs.queries.{Queries, QueriesService}
import org.http4s.{HttpRoutes, Request => Http4sRequest, Response => Http4sResponse, Uri}
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.http4s.blaze.server.BlazeServerBuilder

import cats.effect.unsafe.implicits.global

/** In the real world we would run the different services on distinct
  * machines.
  *
  * But, because this example is just … an example, we want to be able to run
  * everything with a single `sbt run` invocation. So, we run all the
  * services (including serving the web client assets) within the same JVM.
  */
object Main extends App {

  val localhost = "0.0.0.0"

  val (ahc, shutdownClient) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  object commandsService {
    val port = 9001
    val shutdown = Server.start(port, localhost, (new Commands).routes)
  }

  object queriesService {
    val port = 9002
    val shutdown =
      Server.start(
        port,
        localhost, {
          val service = new QueriesService(
            Uri.Authority(host = Uri.RegName(localhost), port = Some(commandsService.port)),
            ahc
          )
          new Queries(service).routes
        }
      )
  }

  object publicService {
    val port = 9090
    val shutdown = Server.start(
      port,
      localhost, {
        new cqrs.publicserver.Router(
          new PublicServer(
            Uri.Authority(host = Uri.RegName(localhost), port = Some(commandsService.port)),
            Uri.Authority(host = Uri.RegName(localhost), port = Some(queriesService.port)),
            ahc
          ),
          new BootstrapEndpoints
        ).routes
      }
    )
  }

  // Start the commands service
  commandsService
  // Start the queries service
  queriesService
  // Start the public server
  publicService

  // …

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = {
      shutdownClient.unsafeRunSync()
      commandsService.shutdown.unsafeRunSync()
      queriesService.shutdown.unsafeRunSync()
      publicService.shutdown.unsafeRunSync()
    }
  })

}

object Server {
  def start(
      port: Int,
      host: String,
      routes: PartialFunction[Http4sRequest[IO], IO[Http4sResponse[IO]]]
  ): IO[Unit] =
    BlazeServerBuilder[IO]
      .bindHttp(port, host)
      .withHttpApp(HttpRoutes.of(routes).orNotFound)
      .allocated
      .unsafeRunSync()
      ._2

}
