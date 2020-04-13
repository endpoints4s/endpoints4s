package endpoints.akkahttp.server

import java.net.ServerSocket

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{
  HttpEntity,
  HttpRequest,
  HttpResponse,
  StatusCodes => AkkaStatusCodes,
  Uri
}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import endpoints.algebra
import endpoints.algebra.server.DecodedUrl

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

class ServerInterpreterTest
    extends algebra.server.EndpointsTestSuite[EndpointsCodecsTestApi]
    with algebra.server.BasicAuthenticationTestSuite[EndpointsCodecsTestApi]
    with algebra.server.ChunkedJsonEntitiesTestSuite[EndpointsCodecsTestApi]
    with algebra.server.TextEntitiesTestSuite[EndpointsCodecsTestApi]
    with ScalatestRouteTest {

  val serverApi = new EndpointsCodecsTestApi

  def serveRoute(route: Route)(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val serverBinding: Future[Http.ServerBinding] =
      Http().bindAndHandle(route, "localhost", port)

    Await.result(serverBinding, 10.seconds)
    try {
      runTests(port)
    } finally {
      Await.result(serverBinding.flatMap(_.unbind()), 10.seconds)
      ()
    }
  }

  def serveEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, Resp],
      response: => Resp
  )(runTests: Int => Unit): Unit =
    serveRoute(endpoint.implementedBy(_ => response))(runTests)

  def serveIdentityEndpoint[Resp](
      endpoint: serverApi.Endpoint[Resp, Resp]
  )(runTests: Int => Unit): Unit =
    serveRoute(endpoint.implementedBy(request => request))(runTests)

  def serveStreamedEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, serverApi.Chunks[Resp]],
      response: Source[Resp, _]
  )(
      runTests: Int => Unit
  ): Unit =
    serveRoute(endpoint.implementedBy(_ => response))(runTests)

  def serveStreamedEndpoint[Req, Resp](
      endpoint: serverApi.Endpoint[serverApi.Chunks[Req], Resp],
      logic: Source[Req, _] => Future[Resp]
  )(
      runTests: Int => Unit
  ): Unit =
    serveRoute(endpoint.implementedByAsync(logic))(runTests)

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    import java.io._

    val directive =
      url.directive.map(a => DecodedUrl.Matched(a)) | [Tuple1[DecodedUrl[A]]] Directives
        .provide(DecodedUrl.NotMatched)
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
      if (status == AkkaStatusCodes.BadRequest) {
        val s = responseAs[String]
        val errors =
          endpoints.ujson.codecs.invalidCodec
            .decode(s)
            .fold(
              _.errors,
              errors =>
                sys.error(
                  s"Unable to parse server response: ${errors.mkString(". ")}"
                )
            )
        DecodedUrl.Malformed(errors)
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

}
