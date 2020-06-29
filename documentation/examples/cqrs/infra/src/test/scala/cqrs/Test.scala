package cqrs

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}

import akka.actor.ActorSystem
import akka.stream.Materializer
import cqrs.commands.Commands
import cqrs.infra.PlayService
import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.publicserver.{BootstrapEndpoints, PublicEndpoints, PublicServer}
import cqrs.queries.{Queries, QueriesService}
import endpoints4s.play.client.{Endpoints, JsonEntitiesFromCodecs}
import org.scalatest.BeforeAndAfterAll
import play.api.Mode
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.api.routing.Router

import scala.collection.immutable.SortedMap
import scala.math.BigDecimal
import org.scalatest.freespec.AsyncFreeSpec

class Test extends AsyncFreeSpec with BeforeAndAfterAll {

  def baseUrl(port: Int): String = s"http://localhost:$port"

  val actorSystem: ActorSystem = ActorSystem()
  val wsClient =
    AhcWSClient(AhcWSClientConfig())(Materializer.matFromSystem(actorSystem))

  object commandsServer extends PlayService(9000, Mode.Test) {
    lazy val commands = new Commands(playComponents)
    lazy val router = Router.from(commands.routes)
  }

  object queriesServer extends PlayService(9001, Mode.Test) {
    lazy val service = new QueriesService(
      baseUrl(commandsServer.port),
      wsClient,
      actorSystem.scheduler
    )
    lazy val queries = new Queries(service, playComponents)
    lazy val router = Router.from(queries.routes)
  }

  object publicServer extends PlayService(9002, Mode.Test) {
    lazy val routes =
      new cqrs.publicserver.Router(
        new PublicServer(
          baseUrl(commandsServer.port),
          baseUrl(queriesServer.port),
          wsClient,
          playComponents
        ),
        new BootstrapEndpoints(playComponents)
      ).routes
    lazy val router = Router.from(routes)
  }

  commandsServer.server
  queriesServer.server
  publicServer.server

  override def afterAll(): Unit = {
    publicServer.server.stop()
    queriesServer.server.stop()
    wsClient.close()
    commandsServer.server.stop()
  }

  object api
      extends Endpoints(baseUrl(publicServer.port), wsClient)
      with JsonEntitiesFromCodecs
      with PublicEndpoints

  "Public server" - {

    "create a new meter and query it" in {
      for {
        meter <- api.createMeter(CreateMeter("Electricity"))
        allMeters <- api.listMeters(())
      } yield assert(allMeters contains meter)
    }

    "create a new meter and add records to it" in {
      val arbitraryDate = OffsetDateTime
        .of(LocalDateTime.of(2017, 1, 8, 12, 34, 56), ZoneOffset.UTC)
        .toInstant
      val arbitraryValue = BigDecimal(10)
      for {
        created <- api.createMeter(CreateMeter("Water"))
        _ <- api.addRecord(
          (created.id, AddRecord(arbitraryDate, arbitraryValue))
        )
        updated <- api.getMeter(created.id)
      } yield assert(
        updated
          .exists(_.timeSeries == SortedMap(arbitraryDate -> arbitraryValue))
      )
    }

  }

}
