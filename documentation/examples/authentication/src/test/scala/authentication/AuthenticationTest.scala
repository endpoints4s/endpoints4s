package authentication

import endpoints4s.play.server.PlayComponents
import org.scalatest.BeforeAndAfterAll
import pdi.jwt.JwtSession
import play.api.{Configuration, Mode}
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.core.server.{NettyServer, ServerConfig}
import org.scalatest.freespec.AsyncFreeSpec

class AuthenticationTest extends AsyncFreeSpec with BeforeAndAfterAll {

  val host = "0.0.0.0"
  val port = 8765
  val playConfig =
    ServerConfig(port = Some(port), mode = Mode.Test, address = host)
  val server = NettyServer
    .fromRouterWithComponents(playConfig) { components =>
      new Server(
        PlayComponents.fromBuiltInComponents(components),
        components.configuration
      ).routes
    }
    .asInstanceOf[NettyServer]
  import server.materializer
  import server.actorSystem.dispatcher
  import ClockSettings._
  implicit val playConfiguration: Configuration = Configuration.reference
  val wsClient = AhcWSClient(AhcWSClientConfig())
  val client = new Client(s"http://$host:$port", wsClient, playConfiguration)

  def uri(path: String): String = s"http://$host:$port$path"

  override def afterAll(): Unit = {
    wsClient.close()
    server.stop()
    super.afterAll()
  }

  "authentication" - {
    "unauthenticated request gets rejected" in {
      for {
        response <- wsClient.url(uri("/some-resource")).get()
      } yield assert(response.status == Status.UNAUTHORIZED)
    }
    "invalid authenticated request gets rejected" in {
      for {
        response <-
          wsClient
            .url(uri("/some-resource"))
            .withHttpHeaders(HeaderNames.AUTHORIZATION -> "lol")
            .get()
      } yield assert(response.status == Status.UNAUTHORIZED)
    }
    "invalid json token gets rejected" in {
      val token =
        """eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"""
      for {
        response <-
          wsClient
            .url(uri("/some-resource"))
            .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $token")
            .get()
      } yield assert(response.status == Status.UNAUTHORIZED)
    }
    "wrong login is rejected" in {
      for {
        loginResponse <-
          wsClient
            .url(uri("/login"))
            .withQueryStringParameters("apiKey" -> "unknown")
            .get()
      } yield assert(loginResponse.status == Status.BAD_REQUEST)
    }
    "login gives a valid json token" in {
      for {
        loginResponse <-
          wsClient
            .url(uri("/login"))
            .withQueryStringParameters("apiKey" -> "foobar")
            .get()
        token =
          loginResponse
            .headers(HeaderNames.AUTHORIZATION)
            .head
            .drop("Bearer ".length)
        _ = {
          assert(loginResponse.status == Status.OK)
          val jwtSession = JwtSession.deserialize(token)
          val user = jwtSession.getAs[UserInfo]("user").get
          assert(user == UserInfo("Alice"))
        }
        response <-
          wsClient
            .url(uri("/some-resource"))
            .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $token")
            .get()
      } yield assert(response.status == Status.OK)
    }
    //#login-test-client
    "wrong login using client" in {
      for {
        loginResult <- client.login("unknown")
      } yield assert(loginResult.isEmpty)
    }
    "valid login using client" in {
      for {
        loginResult <- client.login("foobar")
      } yield assert(loginResult.nonEmpty)
    }
    //#login-test-client
    //#protected-endpoint-test
    "login and access protected resource" in {
      for {
        maybeToken <- client.login("foobar")
        token = maybeToken.get
        _ = assert(token.decoded == UserInfo("Alice"))
        resource <- client.someResource(token)
      } yield assert(resource == "Hello Alice!")
    }
    //#protected-endpoint-test
  }

}
