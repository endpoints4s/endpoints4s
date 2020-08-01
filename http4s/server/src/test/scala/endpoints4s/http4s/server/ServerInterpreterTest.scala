package endpoints4s.http4s.server

import java.net.ServerSocket

import cats.effect.{ContextShift, IO, Timer}
import endpoints4s.{Invalid, Valid}
import endpoints4s.algebra.server.{
  BasicAuthenticationTestSuite,
  DecodedUrl,
  EndpointsTestSuite,
  JsonEntitiesFromSchemasTestSuite,
  SumTypedEntitiesTestSuite,
  TextEntitiesTestSuite
}
import org.http4s.server.Router
import org.http4s.{HttpRoutes, Uri}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._

import scala.concurrent.ExecutionContext
import endpoints4s.algebra.server.AssetsTestSuite
import cats.effect.Blocker

class ServerInterpreterTest
    extends EndpointsTestSuite[EndpointsTestApi]
    with BasicAuthenticationTestSuite[EndpointsTestApi]
    with JsonEntitiesFromSchemasTestSuite[EndpointsTestApi]
    with TextEntitiesTestSuite[EndpointsTestApi]
    with SumTypedEntitiesTestSuite[EndpointsTestApi]
    with AssetsTestSuite[EndpointsTestApi]
    with AssetsResourcesTest {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  val serverApi = new EndpointsTestApi

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

    val service = HttpRoutes.of[IO](endpoint.implementedBy(request2response))
    val httpApp = Router("/" -> service).orNotFound
    val server =
      BlazeServerBuilder[IO](ExecutionContext.global)
        .bindHttp(port, "localhost")
        .withHttpApp(httpApp)
    server.resource.use(_ => IO(runTests(port))).unsafeRunSync()
  }

  def assetsResources(pathPrefix: Option[String]) =
    Blocker[IO]
      .use(blocker => serverApi.Effect.point(serverApi.assetsResources(blocker, pathPrefix)))
      .unsafeRunSync()

  def serveAssetsEndpoint(
      endpoint: serverApi.Endpoint[
        serverApi.AssetRequest,
        serverApi.AssetResponse
      ],
      response: => serverApi.AssetResponse
  )(runTests: Int => Unit): Unit =
    serveGeneralEndpoint(endpoint, (_: Any) => response)(runTests)

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
