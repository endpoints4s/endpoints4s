package quickstart

import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.scalatest.freespec.AsyncFreeSpec

class CounterTest extends AsyncFreeSpec with BeforeAndAfterAll {

  implicit val actorSystem: ActorSystem = ActorSystem()
  val routes = CounterServer.routes ~ DocumentationServer.routes
  val interface = "0.0.0.0"
  val port = findOpenPort()
  val server = Http().newServerAt(interface, port).bindFlow(routes)

  override protected def afterAll(): Unit = {
    Await.result(
      Await.result(server, 10.seconds).terminate(3.seconds),
      15.seconds
    )
    Await.result(actorSystem.terminate(), 5.seconds)
    super.afterAll()
  }

  "CounterServer" - {
    "Query counter value" in {
      for {
        response <- Http().singleRequest(
          HttpRequest(uri = uri("/current-value"))
        )
        entity <- response.entity.toStrict(1.second)
      } yield {
        assert(response.status == StatusCodes.OK)
        assert(entity.contentType == ContentTypes.`application/json`)
        assert(entity.data.utf8String == "{\"value\":0}")
      }
    }
    "Increment counter value" in {
      val request =
        HttpRequest(
          method = HttpMethods.POST,
          uri = uri("/increment"),
          entity = HttpEntity(ContentTypes.`application/json`, "{\"step\":1}")
        )
      for {
        response <- Http().singleRequest(request)
      } yield {
        assert(response.status == StatusCodes.OK)
      }
    }
    "Query API documentation" in {
      for {
        response <- Http().singleRequest(
          HttpRequest(uri = uri("/documentation.json"))
        )
        entity <- response.entity.toStrict(1.second)
      } yield {
        assert(response.status == StatusCodes.OK)
        assert(entity.contentType == ContentTypes.`application/json`)
      }
    }
  }

  def findOpenPort(): Int = {
    val socket = new ServerSocket(0)
    try socket.getLocalPort
    finally if (socket != null) socket.close()
  }

  def uri(suffix: String) = s"http://$interface:$port$suffix"

}
