package cqrs

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.effect.testing.scalatest.AsyncIOSpec

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import cqrs.commands.Commands
import cqrs.infra.Server
import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.publicserver.{BootstrapEndpoints, PublicEndpoints, PublicServer}
import cqrs.queries.{Queries, QueriesService}
import endpoints4s.http4s.client.{Endpoints, JsonEntitiesFromCodecs}
import org.http4s.Uri
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.scalatest.BeforeAndAfterAll

import scala.collection.immutable.SortedMap
import scala.math.BigDecimal
import org.scalatest.freespec.AsyncFreeSpec

import scala.concurrent.ExecutionContext

class Test extends AsyncFreeSpec with AsyncIOSpec with BeforeAndAfterAll {

  // See https://github.com/typelevel/cats-effect-testing/issues/145
  override implicit def executionContext: ExecutionContext = IORuntime.global.compute

  val (ahc, shutdownClient) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  val commandsServerPort = 9090
  val queriesServerPort = 9091
  val publicServerPort = 9092
  val localhost = "0.0.0.0"

  val commandsShutdown: IO[Unit] =
    Server.start(commandsServerPort, localhost, (new Commands).routes)

  val queriesShutdown: IO[Unit] =
    Server.start(queriesServerPort, localhost, {
      val service = new QueriesService(
        Uri.Authority(host = Uri.RegName(localhost), port = Some(commandsServerPort)),
        ahc
      )
      new Queries(service).routes
    })

  val publicServerShutdown: IO[Unit] =
    Server.start(publicServerPort, localhost, {
      new cqrs.publicserver.Router(
        new PublicServer(
          Uri.Authority(host = Uri.RegName(localhost), port = Some(commandsServerPort)),
          Uri.Authority(host = Uri.RegName(localhost), port = Some(queriesServerPort)),
          ahc
        ),
        new BootstrapEndpoints
      ).routes
    })

  override def afterAll(): Unit = {
    shutdownClient.unsafeRunSync()
    publicServerShutdown.unsafeRunSync()
    queriesShutdown.unsafeRunSync()
    commandsShutdown.unsafeRunSync()
  }

  object api
      extends Endpoints(
        Uri.Authority(host = Uri.RegName(localhost), port = Some(publicServerPort)),
        Uri.Scheme.http,
        ahc
      )
      with JsonEntitiesFromCodecs
      with PublicEndpoints

  "Public server" - {

    "create a new meter and query it" in {
      for {
        meter <- api.createMeter.sendAndConsume(CreateMeter("Electricity"))
        allMeters <- api.listMeters.sendAndConsume(())
      } yield assert(allMeters contains meter)
    }

    "create a new meter and add records to it" in {
      val arbitraryDate = OffsetDateTime
        .of(LocalDateTime.of(2017, 1, 8, 12, 34, 56), ZoneOffset.UTC)
        .toInstant
      val arbitraryValue = BigDecimal(10)
      for {
        created <- api.createMeter.sendAndConsume(CreateMeter("Water"))
        _ <- api.addRecord.sendAndConsume(
          (created.id, AddRecord(arbitraryDate, arbitraryValue))
        )
        updated <- api.getMeter.sendAndConsume(created.id)
      } yield assert(
        updated
          .exists(_.timeSeries == SortedMap(arbitraryDate -> arbitraryValue))
      )
    }

  }

}
