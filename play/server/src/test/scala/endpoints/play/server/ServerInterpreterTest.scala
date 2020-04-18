package endpoints.play.server

import java.net.ServerSocket

import akka.stream.scaladsl.Source
import endpoints.{Invalid, Valid}
import endpoints.algebra.server.{
  BasicAuthenticationTestSuite,
  DecodedUrl,
  EndpointsTestSuite,
  ChunkedJsonEntitiesTestSuite,
  TextEntitiesTestSuite
}
import play.api.Mode
import play.api.routing.Router
import play.api.test.FakeRequest
import play.core.server.{
  DefaultNettyServerComponents,
  NettyServer,
  ServerConfig
}

import scala.concurrent.Future

class ServerInterpreterTest
    extends EndpointsTestSuite[EndpointsTestApi]
    with BasicAuthenticationTestSuite[EndpointsTestApi]
    with ChunkedJsonEntitiesTestSuite[EndpointsTestApi]
    with TextEntitiesTestSuite[EndpointsTestApi] {

  val serverApi: EndpointsTestApi = {
    object NettyServerComponents extends DefaultNettyServerComponents {
      override lazy val serverConfig = ServerConfig(mode = Mode.Test)
      lazy val router = Router.empty
    }
    new EndpointsTestApi(
      PlayComponents.fromBuiltInComponents(NettyServerComponents),
      Map.empty
    )
  }

  def serveEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, Resp],
      response: => Resp
  )(runTests: Int => Unit): Unit =
    serveRoutes(
      serverApi.routesFromEndpoints(endpoint.implementedBy(_ => response))
    )(runTests)

  def serveIdentityEndpoint[Resp](
      endpoint: serverApi.Endpoint[Resp, Resp]
  )(runTests: Int => Unit): Unit =
    serveRoutes(
      serverApi.routesFromEndpoints(endpoint.implementedBy(request => request))
    )(runTests)

  def serveStreamedEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, serverApi.Chunks[Resp]],
      response: Source[Resp, _]
  )(runTests: Int => Unit): Unit =
    serveRoutes(
      serverApi.routesFromEndpoints(endpoint.implementedBy(_ => response))
    )(runTests)

  def serveStreamedEndpoint[Req, Resp](
      endpoint: serverApi.Endpoint[serverApi.Chunks[Req], Resp],
      logic: Source[Req, _] => Future[Resp]
  )(
      runTests: Int => Unit
  ): Unit =
    serveRoutes(
      serverApi.routesFromEndpoints(endpoint.implementedByAsync(logic))
    )(runTests)

  def serveRoutes(routes: Router.Routes)(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val config = ServerConfig(mode = Mode.Test, port = Some(port))
    val server = NettyServer.fromRouterWithComponents(config)(_ => routes)
    try {
      runTests(port)
    } finally {
      server.stop()
    }
  }

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val request = FakeRequest("GET", rawValue)
    url.decodeUrl(request) match {
      case None                  => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a))        => DecodedUrl.Matched(a)
    }
  }

}
