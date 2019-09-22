package endpoints.play.server

import endpoints.algebra.server.{DecodedUrl, EndpointsTestSuite}
import play.api.Mode
import play.api.routing.Router
import play.api.test.FakeRequest
import play.core.server.{DefaultNettyServerComponents, ServerConfig}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ServerInterpreterTest extends EndpointsTestSuite[Endpoints] with DefaultNettyServerComponents {

  override lazy val serverConfig = ServerConfig(mode = Mode.Test)
  lazy val router = Router.empty // We don’t use the server, we just want the BuiltInComponents to be wired for us
  lazy val playComponents = PlayComponents.fromBuiltInComponents(this)
  lazy val serverApi: Endpoints = new EndpointsTestApi(playComponents, Map.empty)

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val request = FakeRequest("GET", rawValue)
    url.decodeUrl(request) match {
      case None           => DecodedUrl.NotMatched
      case Some(Left(_))  => DecodedUrl.Malformed
      case Some(Right(a)) => DecodedUrl.Matched(a)
    }
  }

  urlsTestSuite()

  override protected def afterAll(): Unit = {
    Await.ready(actorSystem.terminate(), 10.seconds)
    super.afterAll()
  }

}
