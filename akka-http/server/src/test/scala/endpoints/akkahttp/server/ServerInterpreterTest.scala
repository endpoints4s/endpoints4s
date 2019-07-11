package endpoints.akkahttp.server

import java.net.ServerSocket

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.{Flow, Source}
import endpoints.akkahttp.server
import endpoints.algebra.server.{DecodedUrl, EndpointsTestSuite, Http1JsonStreamingTestSuite}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ServerInterpreterTest
  extends EndpointsTestSuite[EndpointsCodecsTestApi]
    with Http1JsonStreamingTestSuite[EndpointsCodecsTestApi]
    with ScalatestRouteTest {

  val settings = new server.Http1Streaming.Settings(system.dispatcher, materializer, 5.seconds)
  val serverApi = new EndpointsCodecsTestApi(settings)

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    import java.io._

    val directive =
      url.directive.map(a => DecodedUrl.Matched(a)) |[Tuple1[DecodedUrl[A]]] Directives.extract(_ => DecodedUrl.NotMatched)
    val route = directive { decodedA => req =>
      val baos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(baos)
      try {
        oos.writeObject(decodedA)
        val response = HttpResponse(entity = HttpEntity(baos.toByteArray))
        req.complete(response)
      } finally {
        if (baos != null) baos.close()
        if (oos != null) oos.close()
      }
    }
    val request = HttpRequest(uri = Uri(rawValue))
    request ~> route ~> check {
      if (status == StatusCodes.BadRequest) {
        DecodedUrl.Malformed
      } else {
        val bs = responseAs[Array[Byte]]
        val bais = new ByteArrayInputStream(bs)
        val ois = new ObjectInputStream(bais)
        try {
          ois.readObject.asInstanceOf[DecodedUrl[A]]
        } finally {
          if (bais != null) bais.close()
          if (ois != null) ois.close()
        }
      }
    }
  }

  def serveRoute(route: Route)(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val serverBinding: Future[Http.ServerBinding] =
      Http().bindAndHandle(route, "localhost", port)

    Await.result(serverBinding, 10.seconds)
    runTests(port)
    Await.result(serverBinding.flatMap(_.unbind()), 10.seconds)
    ()
  }

  def serveEndpoint[Resp](
    endpoint: serverApi.Endpoint[_, Resp],
    response: Resp
  )(
    runTests: Int => Unit
  ): Unit =
    serveRoute(endpoint.implementedBy(_ => response))(runTests)

  def serveChunkedEndpoint[Resp](
    endpoint: serverApi.ChunkedEndpoint[_, Resp],
    response: Source[Resp, _]
  )(
    runTests: Int => Unit
  ): Unit =
    serveRoute(endpoint.implementedBy(_ => response))(runTests)

  def serveWebSocketEndpoint[Req, Resp](
    endpoint: serverApi.WebSocketEndpoint[_, Req, Resp],
    serverFlow: Flow[Req, Resp, _]
  )(
    runTests: Int => Unit
  ): Unit =
    serveRoute(endpoint.implementedBy(_ => Future.successful(Some(serverFlow))))(runTests)

}
