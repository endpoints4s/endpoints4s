package cqrs

import java.net.URLEncoder
import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset}
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import cqrs.commands.Commands
import cqrs.infra.PlayService
import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.publicserver.{PublicEndpoints, PublicServer}
import cqrs.queries.{Queries, QueriesService}
import endpoints.play.client.{JsonEntitiesFromCodec, Endpoints}
import endpoints.play.server.HttpServer
import org.scalatest.{AsyncFreeSpec, BeforeAndAfterAll}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}

import scala.collection.immutable.SortedMap
import scala.math.BigDecimal

class Test extends AsyncFreeSpec with BeforeAndAfterAll {

  def baseUrl(port: Int): String = s"http://localhost:$port"

  val actorSystem: ActorSystem = ActorSystem()
  val materializer: Materializer = ActorMaterializer()(actorSystem)
  val wsClient = AhcWSClient(AhcWSClientConfig())(materializer)

  object commandsServer extends PlayService(9000) {
    val commands = new Commands(playComponents)
    val httpServer = HttpServer(config, playComponents, commands.routes)
  }

  object queriesServer extends PlayService (9001) {
    val service = new QueriesService(baseUrl(commandsServer.port), wsClient, actorSystem.scheduler)
    val queries = new Queries(service, playComponents)
    val httpServer = HttpServer(config, playComponents, queries.routes)
  }

  object publicServer extends PlayService(9002) {
    val server = new PublicServer(baseUrl(commandsServer.port), baseUrl(queriesServer.port), wsClient, playComponents)
    val httpServer = HttpServer(config, playComponents, server.routes)
  }

  commandsServer
  queriesServer
  publicServer

  override def afterAll(): Unit = {
    publicServer.httpServer.stop()
    wsClient.close()
    queriesServer.httpServer.stop()
    commandsServer.httpServer.stop()
  }

  object api
    extends Endpoints(baseUrl(publicServer.port), wsClient)
      with JsonEntitiesFromCodec
      with PublicEndpoints {

    def uuidSegment: Segment[UUID] =
      (uuid: UUID) => URLEncoder.encode(uuid.toString, utf8Name)

  }

  "Public server" - {

    "create a new meter and query it" in {
      for {
        meter     <- api.createMeter(CreateMeter("Electricity"))
        allMeters <- api.listMeters(())
      } yield assert(allMeters contains meter)
    }

    "create a new meter and add records to it" in {
      val arbitraryDate = OffsetDateTime.of(LocalDateTime.of(2017, 1, 8, 12, 34, 56), ZoneOffset.UTC).toInstant
      val arbitraryValue = BigDecimal(10)
      for {
        created <- api.createMeter(CreateMeter("Water"))
        _       <- api.addRecord((created.id, AddRecord(arbitraryDate, arbitraryValue)))
        updated <- api.getMeter(created.id)
      } yield assert(updated.exists(_.timeSeries == SortedMap(arbitraryDate -> arbitraryValue)))
    }

  }

}
