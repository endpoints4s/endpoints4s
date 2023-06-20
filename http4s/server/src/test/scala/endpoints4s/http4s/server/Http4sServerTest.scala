package endpoints4s.http4s.server

import java.net.ServerSocket

import cats.effect.IO
import endpoints4s.{Invalid, Valid, algebra}
import endpoints4s.algebra.server.DecodedUrl
import org.http4s.Uri
import org.http4s.server.Router
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder

import cats.effect.unsafe.implicits.global

/**
 * Base class for http4s server interpreters tests.
 */
trait Http4sServerTest[T <: Endpoints[IO]]
  extends algebra.server.ServerTestBase[T] {

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val uri =
      Uri.fromString(rawValue).getOrElse(sys.error(s"Illegal URI: $rawValue"))

    url.decodeUrl(uri) match {
      case None => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a)) => DecodedUrl.Matched(a)
    }
  }

  private def serveGeneralEndpoint[Req, Resp](
      endpoint: serverApi.Endpoint[Req, Resp],
      request2response: Req => Resp
  )(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }

    val service = HttpRoutes.of[IO](endpoint.implementedBy(request2response))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO]
        .bindHttp(port, "localhost")
        .withHttpApp(httpApp)
    server.resource.use(_ => IO(runTests(port))).unsafeRunSync()
  }

  def serveEndpoint[Req, Resp](
      endpoint: serverApi.Endpoint[Req, Resp],
      response: => Resp
  )(runTests: Int => Unit): Unit =
    serveGeneralEndpoint(endpoint, (_: Any) => response)(runTests)

  def serveIdentityEndpoint[Resp](
      endpoint: serverApi.Endpoint[Resp, Resp]
  )(runTests: Int => Unit): Unit =
    serveGeneralEndpoint(endpoint, identity[Resp])(runTests)
}
