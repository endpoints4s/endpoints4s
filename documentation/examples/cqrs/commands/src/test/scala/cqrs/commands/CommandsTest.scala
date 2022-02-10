package cqrs.commands

import cats.effect.IO

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID
import org.scalatest.BeforeAndAfterAll
import endpoints4s.http4s.client.{Endpoints, JsonEntitiesFromCodecs}
import org.http4s.{HttpRoutes, Uri}

import scala.math.BigDecimal
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

class CommandsTest extends AsyncFreeSpec with AsyncIOSpec with BeforeAndAfterAll {

  // See https://github.com/typelevel/cats-effect-testing/issues/145
  override implicit def executionContext: ExecutionContext = IORuntime.global.compute

  val host = "0.0.0.0"
  val port = 9000
  val (server, shutdownServer) = BlazeServerBuilder[IO]
    .bindHttp(port, host)
    .withHttpApp(HttpRoutes.of((new Commands).routes).orNotFound)
    .allocated
    .unsafeRunSync()
  val (ahc, shutdownClient) =
    AsyncHttpClient.allocate[IO]().unsafeRunSync()

  object client
      extends Endpoints(
        Uri.Authority(host = Uri.RegName(host), port = Some(port)),
        Uri.Scheme.http,
        ahc
      )
      with JsonEntitiesFromCodecs
      with CommandsEndpoints

  override def afterAll(): Unit = {
    shutdownClient.unsafeRunSync()
    shutdownServer.unsafeRunSync()
    super.afterAll()
  }

  "Commands" - {

    val arbitraryDate = OffsetDateTime
      .of(LocalDateTime.of(2017, 1, 8, 12, 34, 56), ZoneOffset.UTC)
      .toInstant
    val arbitraryValue = BigDecimal(10)

    "create a new meter" in {
      client.command.sendAndConsume(CreateMeter("electricity")).map { maybeEvent =>
        assert(maybeEvent.collect { case StoredEvent(_, MeterCreated(_, "electricity")) =>
          ()
        }.nonEmpty)
      }
    }
    "create a meter and add readings to it" in {
      for {
        maybeCreatedEvent <- client.command.sendAndConsume(CreateMeter("water"))
        id <-
          maybeCreatedEvent
            .collect { case StoredEvent(_, MeterCreated(id, _)) => id }
            .fold[IO[UUID]](IO.raiseError(new NoSuchElementException))(IO.pure)
        maybeAddedEvent <- client.command.sendAndConsume(
          AddRecord(id, arbitraryDate, arbitraryValue)
        )
        _ <-
          maybeAddedEvent
            .collect {
              case StoredEvent(
                    _,
                    RecordAdded(`id`, `arbitraryDate`, `arbitraryValue`)
                  ) =>
                ()
            }
            .fold[IO[Unit]](IO.raiseError(new NoSuchElementException))(IO.pure)
      } yield assert(true)
    }
  }

}
