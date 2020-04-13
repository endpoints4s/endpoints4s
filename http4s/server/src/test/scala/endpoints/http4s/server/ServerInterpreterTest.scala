package endpoints.http4s.server

import java.net.ServerSocket

import cats.effect.{ContextShift, IO, Timer}
import endpoints.{Invalid, Valid}
import endpoints.algebra.server.{
  BasicAuthenticationTestSuite,
  DecodedUrl,
  EndpointsTestSuite,
  JsonEntitiesFromSchemasTestSuite,
  TextEntitiesTestSuite
}
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Uri}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext

class ServerInterpreterTest
    extends EndpointsTestSuite[EndpointsTestApi]
    with BasicAuthenticationTestSuite[EndpointsTestApi]
    with JsonEntitiesFromSchemasTestSuite[EndpointsTestApi]
    with TextEntitiesTestSuite[EndpointsTestApi] {

  val serverApi = new EndpointsTestApi()

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val uri =
      Uri.fromString(rawValue).getOrElse(sys.error(s"Illegal URI: $rawValue"))

    url.decodeUrl(uri) match {
      case None                  => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a))        => DecodedUrl.Matched(a)
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
    implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
    val service = HttpRoutes.of[IO](endpoint.implementedBy(request2response))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO].bindHttp(port, "localhost").withHttpApp(httpApp)
    val fiber = server.resource.use(_ => IO.never).start.unsafeRunSync()
    try {
      runTests(port)
    } finally {
      fiber.cancel.unsafeRunSync()
    }
  }

  def serveEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, Resp],
      response: => Resp
  )(runTests: Int => Unit): Unit =
    serveGeneralEndpoint(endpoint, (_: Any) => response)(runTests)

  def serveIdentityEndpoint[Resp](
      endpoint: serverApi.Endpoint[Resp, Resp]
  )(runTests: Int => Unit): Unit =
    serveGeneralEndpoint(endpoint, identity[Resp])(runTests)
}
