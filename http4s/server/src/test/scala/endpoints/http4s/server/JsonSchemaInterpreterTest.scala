package endpoints.http4s.server

import java.net.ServerSocket

import akka.actor.ActorSystem
import cats.effect.{ContextShift, IO, Timer}
import endpoints.algebra.server.{DecodedUrl, JsonEntitiesFromSchemasTestSuite}
import endpoints.{Invalid, Valid}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import org.http4s.{HttpRoutes, Uri}
import scala.concurrent.ExecutionContext

class JsonSchemaInterpreterTest extends JsonEntitiesFromSchemasTestSuite[JsonSchemaEndpointsTestApi] {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val system = ActorSystem()

  val serverApi = new JsonSchemaEndpointsTestApi()

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val uri =
      Uri.fromString(rawValue).getOrElse(sys.error(s"Illegal URI: $rawValue"))

    url.decodeUrl(uri) match {
      case None                  => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a))        => DecodedUrl.Matched(a)
    }
  }

  def serveEndpoint[Resp](
      endpoint: serverApi.Endpoint[_, Resp],
      response: => Resp
  )(runTests: Int => Unit): Unit = {
    val port = {
      val socket = new ServerSocket(0)
      try socket.getLocalPort
      finally if (socket != null) socket.close()
    }
    val service = HttpRoutes.of[IO](endpoint.implementedBy(_ => response))
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

}
