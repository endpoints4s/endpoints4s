package endpoints.play.server

import akka.actor.CoordinatedShutdown.UnknownReason
import endpoints.{Invalid, Valid}
import endpoints.algebra.server.{DecodedUrl, EndpointsTestSuite}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.libs.ws.{WSAuthScheme, WSResponse}
import play.api.Mode
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.core.server.{DefaultNettyServerComponents, ServerConfig}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ServerInterpreterTest
  extends EndpointsTestSuite[Endpoints]
    with DefaultNettyServerComponents
    with PlayComponents
    with AhcWSComponents {

  override lazy val serverConfig = ServerConfig(mode = Mode.Test, port = Some(testServerPort))
  object serverApi extends EndpointsTestApi(this, Map.empty) {
    val routes = routesFromEndpoints(
      protectedEndpoint.implementedBy { credentials =>
        if (credentials.username == "admin") Some("Hello!") else None
      },
      protectedEndpointWithParameter.implementedBy { case (id, credentials) =>
        if (credentials.username == "admin") Some(s"Requested user $id") else None
      },
      smokeEndpoint.implementedBy(_ => sys.error("Sorry."))
    )
  }
  lazy val router = Router.from(serverApi.routes)

  // Make sure the server is started
  server

  def decodeUrl[A](url: serverApi.Url[A])(rawValue: String): DecodedUrl[A] = {
    val request = FakeRequest("GET", rawValue)
    url.decodeUrl(request) match {
      case None                  => DecodedUrl.NotMatched
      case Some(Invalid(errors)) => DecodedUrl.Malformed(errors)
      case Some(Valid(a))        => DecodedUrl.Matched(a)
    }
  }

  override protected def afterAll(): Unit = {
    Await.ready(coordinatedShutdown.run(UnknownReason), 10.seconds)
    super.afterAll()
  }

  urlsTestSuite()

  "Authenticated routes" should {

    "reject unauthenticated requests" in {
      val response: WSResponse =
        await(wsClient.url(s"http://localhost:$testServerPort/users").get())
      response.status shouldBe UNAUTHORIZED
      response.headers.get("www-authenticate").flatMap(_.headOption) shouldBe Some("Basic realm=Realm")
      response.body shouldBe ""
    }

    "accept authenticated requests" in {
      val response: WSResponse =
        await(
          wsClient
            .url(s"http://localhost:$testServerPort/users")
            .withAuth("admin", "foo", WSAuthScheme.BASIC)
            .get()
        )
      response.status shouldBe OK
      response.body shouldBe "Hello!"
    }

    "forbid authenticated requests with insufficient rights" in {
      val response: WSResponse =
        await(
          wsClient
            .url(s"http://localhost:$testServerPort/users")
            .withAuth("alice", "foo", WSAuthScheme.BASIC)
            .get()
        )
      response.status shouldBe FORBIDDEN
      response.body shouldBe ""
    }

    "reject unauthenticated requests with invalid parameters before handling authorization" in {
      val response: WSResponse =
        await(wsClient.url(s"http://localhost:$testServerPort/users/foo").get())
      response.status shouldBe BAD_REQUEST
      response.body shouldBe "[\"Invalid integer value 'foo' for segment 'id'\"]"
    }

  }

  "Routes" should {

    "Handle exceptions by default" in {
      val response: WSResponse =
        await(wsClient.url(s"http://localhost:$testServerPort/user/foo/description?name=a&age=1").get())
      response.status shouldBe INTERNAL_SERVER_ERROR
      response.body shouldBe "[\"Sorry.\"]"
    }

  }

}
