package cqrs.commands

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID

import akka.stream.Materializer
import org.scalatest.{AsyncFreeSpec, BeforeAndAfterAll}
import endpoints.play.client.{JsonEntitiesFromCodec, Endpoints}
import endpoints.play.server.DefaultPlayComponents
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.core.server.{NettyServer, ServerConfig}

import scala.concurrent.Future
import scala.math.BigDecimal

class CommandsTest extends AsyncFreeSpec with BeforeAndAfterAll {

  private val server = NettyServer.fromRouter()(new Commands(new DefaultPlayComponents(ServerConfig())).routes)

  implicit val materializer: Materializer = server.materializer
  private val wsClient = AhcWSClient(AhcWSClientConfig())

  object client
    extends Endpoints("http://localhost:9000", wsClient)
      with JsonEntitiesFromCodec
      with CommandsEndpoints

  override def afterAll(): Unit = {
    server.stop()
    wsClient.close()
  }

  "Commands" - {

    val arbitraryDate = OffsetDateTime.of(LocalDateTime.of(2017, 1, 8, 12, 34, 56), ZoneOffset.UTC).toInstant
    val arbitraryValue = BigDecimal(10)

    "create a new meter" in {
      client.command(CreateMeter("electricity")).map { maybeEvent =>
        assert(maybeEvent.collect { case StoredEvent(_, MeterCreated(_, "electricity")) => () }.nonEmpty)
      }
    }
    "create a meter and add readings to it" in {
      for {
        maybeCreatedEvent <- client.command(CreateMeter("water"))
        id <-
          maybeCreatedEvent
            .collect { case StoredEvent(_, MeterCreated(id, _)) => id }
            .fold[Future[UUID]](Future.failed(new NoSuchElementException))(Future.successful)
        maybeAddedEvent <- client.command(AddRecord(id, arbitraryDate, arbitraryValue))
        _ <-
          maybeAddedEvent
            .collect { case StoredEvent(_, RecordAdded(`id`, `arbitraryDate`, `arbitraryValue`)) => () }
            .fold[Future[Unit]](Future.failed(new NoSuchElementException))(Future.successful)
      } yield assert(true)
    }
  }

}
