package endpoints.play.server

import endpoints.algebra.server.{DecodedUrl, EndpointsTestSuite}
import play.api.Mode
import play.api.test.FakeRequest
import play.core.server.ServerConfig

class ServerInterpreterTest extends EndpointsTestSuite[Endpoints] {

  val config = ServerConfig(mode = Mode.Test)
  val serverApi: Endpoints = new EndpointsTestApi(new DefaultPlayComponents(config), Map.empty)

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val request = FakeRequest("GET", rawValue)
    url.decodeUrl(request) match {
      case None           => DecodedUrl.NotMatched
      case Some(Left(_))  => DecodedUrl.Malformed
      case Some(Right(a)) => DecodedUrl.Matched(a)
    }
  }

  urlsTestSuite()

}
