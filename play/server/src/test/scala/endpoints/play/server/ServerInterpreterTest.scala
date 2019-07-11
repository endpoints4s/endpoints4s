package endpoints.play.server

import java.net.ServerSocket

import akka.stream.scaladsl.{Flow, Source}
import endpoints.algebra.server.{DecodedUrl, EndpointsTestSuite, Http1JsonStreamingTestSuite}
import play.api.Mode
import play.api.routing.Router
import play.api.test.FakeRequest
import play.core.server.ServerConfig

import scala.concurrent.Future

class ServerInterpreterTest
  extends EndpointsTestSuite[EndpointsTestApi]
    with Http1JsonStreamingTestSuite[EndpointsTestApi] {

  val serverApi: EndpointsTestApi = new EndpointsTestApi(new DefaultPlayComponents(ServerConfig(mode = Mode.Test)), Map.empty)

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val request = FakeRequest("GET", rawValue)
    url.decodeUrl(request) match {
      case None           => DecodedUrl.NotMatched
      case Some(Left(_))  => DecodedUrl.Malformed
      case Some(Right(a)) => DecodedUrl.Matched(a)
    }
  }

  def serveEndpoint[Resp](endpoint: serverApi.Endpoint[_, Resp], response: Resp)(runTests: Int => Unit): Unit =
    serveRoutes(serverApi.routesFromEndpoints(endpoint.implementedBy(_ => response)))(runTests)

  def serveChunkedEndpoint[Resp](endpoint: serverApi.ChunkedEndpoint[_, Resp], response: Source[Resp, _])(runTests: Int => Unit): Unit =
    serveRoutes(
      serverApi.routesFromEndpoints(endpoint.implementedBy(_ => response))
    )(runTests)

  def serveWebSocketEndpoint[Req, Resp](
    endpoint: serverApi.WebSocketEndpoint[_, Req, Resp],
    serverFlow: Flow[Req, Resp, _]
  )(
    runTests: Int => Unit
  ): Unit =
    serveRoutes(
      serverApi.routesFromEndpoints(
        endpoint.implementedBy(_ => Future.successful(Some(serverFlow)))
      )
    )(runTests)

  def serveRoutes(routes: Router.Routes)(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val config = ServerConfig(mode = Mode.Test, port = Some(port))
    val server = HttpServer(config, new DefaultPlayComponents(config), routes)
    runTests(port)
    server.stop()
  }

}
