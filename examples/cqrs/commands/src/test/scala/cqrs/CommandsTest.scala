package cqrs

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.{AsyncFreeSpec, BeforeAndAfterAll}
import play.api.{Environment, Mode}
import play.api.inject.{ApplicationLifecycle, DefaultApplicationLifecycle}
import play.api.libs.ws.{WSAPI, WSClient, WSClientConfig}
import play.api.libs.ws.ahc.{AhcWSAPI, AhcWSClientConfig}
import play.core.server.NettyServer
import endpoints.play.client.{CirceEntities, Endpoints}

import scala.concurrent.Future
import scala.math.BigDecimal

class CommandsTest extends AsyncFreeSpec with BeforeAndAfterAll {

  private val server = NettyServer.fromRouter()(Commands.routes)

  implicit val actorSystem: ActorSystem = server.actorSystem
  implicit val materializer: Materializer = server.materializer
  val wsComponents = new AhcWSComponents

  object client
    extends Endpoints("http://localhost:9000", wsComponents.wsClient)
      with CirceEntities
      with CommandsEndpoints

  override def afterAll(): Unit = {
    server.stop()
  }

  "Commands" - {

    val arbitraryDate = OffsetDateTime.of(LocalDateTime.of(2017, 1, 8, 12, 34, 56), ZoneOffset.UTC)
    val arbitraryValue = BigDecimal(10)

    "create a new meter" in {
      client.command(CreateMeter).map { maybeEvent =>
        assert(maybeEvent.collect { case MeterCreated(_) => () }.nonEmpty)
      }
    }
    "create a meter and add readings to it" in {
      for {
        maybeCreatedEvent <- client.command(CreateMeter)
        id <-
          maybeCreatedEvent
            .collect { case MeterCreated(id) => id }
            .fold[Future[UUID]](Future.failed(new NoSuchElementException))(Future.successful)
        maybeAddedEvent <- client.command(AddRecord(id, arbitraryDate, arbitraryValue))
        _ <-
          maybeAddedEvent
            .collect { case RecordAdded(`arbitraryDate`, `arbitraryValue`) => () }
            .fold[Future[Unit]](Future.failed(new NoSuchElementException))(Future.successful)
      } yield assert(true)
    }
  }

}

class AhcWSComponents(implicit materializer: Materializer) {
  val applicationLifecycle = new DefaultApplicationLifecycle
  val environment = Environment.simple(mode = Mode.Prod)
  lazy val wsClientConfig: WSClientConfig = WSClientConfig()
  lazy val ahcWsClientConfig: AhcWSClientConfig = AhcWSClientConfig()
  lazy val wsApi: WSAPI = new AhcWSAPI(environment, ahcWsClientConfig, applicationLifecycle)
  lazy val wsClient: WSClient = wsApi.client
}
