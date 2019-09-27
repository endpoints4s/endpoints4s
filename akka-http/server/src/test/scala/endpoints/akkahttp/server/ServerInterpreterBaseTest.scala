package endpoints.akkahttp.server

import java.net.ServerSocket

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import endpoints.algebra.server.{DecodedUrl, ServerTestBase}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ServerInterpreterBaseTest(val serverApi: EndpointsTestApi)
  extends ServerTestBase[EndpointsTestApi]
    with ScalatestRouteTest {

  def serveEndpoint[Resp](endpoint: serverApi.Endpoint[_, Resp], response: Resp)(runTests: Int => Unit): Unit = {

    val routes: Route = endpoint.implementedBy(_ => response)
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val serverBinding: Future[Http.ServerBinding] =
      Http().bindAndHandle(routes, "localhost", port)

    Await.result(serverBinding, 10.seconds)
    runTests(port)
    Await.result(serverBinding.flatMap(_.unbind()), 10.seconds)
    ()
  }

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    import java.io._

    val directive =
      url.directive.map(a => DecodedUrl.Matched(a)) |[Tuple1[DecodedUrl[A]]] Directives.provide(DecodedUrl.NotMatched)
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
        val s = responseAs[String]
        val errors = s.drop(2).dropRight(2).split("\",\"")
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
