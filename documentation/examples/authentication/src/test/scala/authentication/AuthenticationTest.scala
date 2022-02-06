package authentication

import cats.effect.IO
import org.http4s.{AuthScheme, Credentials, Header, HttpRoutes, Request, Status, Uri}
import org.http4s.asynchttpclient.client.AsyncHttpClient
import org.http4s.blaze.server.BlazeServerBuilder
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.unsafe.IORuntime
import org.http4s.headers.Authorization
import org.typelevel.ci.CIString
import pdi.jwt.JwtCirce

import scala.concurrent.ExecutionContext

class AuthenticationTest extends AsyncFreeSpec with AsyncIOSpec with BeforeAndAfterAll {

  // See https://github.com/typelevel/cats-effect-testing/issues/145
  override implicit def executionContext: ExecutionContext = IORuntime.global.compute

  val host = "0.0.0.0"
  val port = 8765
  val (server, shutdownServer) = BlazeServerBuilder[IO]
    .bindHttp(port, host)
    .withHttpApp(HttpRoutes.of((new Server).routes).orNotFound)
    .allocated
    .unsafeRunSync()
  val (ahc, shutdownClient) = AsyncHttpClient.allocate[IO]().unsafeRunSync()
  val client = new Client(
    Uri.Authority(host = Uri.RegName(host), port = Some(port)),
    Uri.Scheme.http,
    ahc
  )

  def uri(path: String): Uri = Uri.unsafeFromString(s"http://$host:$port$path")

  override def afterAll(): Unit = {
    shutdownClient.unsafeRunSync()
    shutdownServer.unsafeRunSync()
    super.afterAll()
  }

  "authentication" - {
    "unauthenticated request gets rejected" in {
      for {
        status <- ahc.statusFromUri(uri("/some-resource"))
      } yield assert(status == Status.Unauthorized)
    }
    "invalid authenticated request gets rejected" in {
      val request = Request[IO]()
        .withUri(uri("/some-resource"))
        .putHeaders(Header.Raw(CIString("Authorization"), "lol"))
      for {
        status <- ahc.status(request)
      } yield assert(status == Status.Unauthorized)
    }
    "invalid json token gets rejected" in {
      val token =
        """eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"""
      val request = Request[IO]()
        .withUri(uri("/some-resource"))
        .putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token)))
      for {
        status <- ahc.status(request)
      } yield assert(status == Status.Unauthorized)
    }
    "wrong login is rejected" in {
      val request = Request[IO]()
        .withUri(uri("/login").withQueryParam("apiKey", "unknown"))
      for {
        status <- ahc.status(request)
      } yield assert(status == Status.BadRequest)
    }
    "login gives a valid json token" in {
      ahc.get(uri("/login").withQueryParam("apiKey", "foobar")) { response =>
        assert(response.status == Status.Ok)
        val responseJson = io.circe.parser.parse(response.as[String].unsafeRunSync()).toOption.get
        val token = responseJson.hcursor.downField("jwt_token").as[String].toOption.get
        val claim = JwtCirce.decode(token, Keys.pair.getPublic).get
        val userInfo = io.circe.parser.parse(claim.content).flatMap(_.as[UserInfo]).toOption.get
        assert(userInfo == UserInfo("Alice"))

        val authenticatedRequest = Request[IO]()
          .withUri(uri("/some-resource"))
          .putHeaders(Authorization(Credentials.Token(AuthScheme.Bearer, token)))
        IO.pure(assert(ahc.successful(authenticatedRequest).unsafeRunSync()))
      }
    }
    //#login-test-client
    "wrong login using client" in {
      for {
        loginResult <- client.login.sendAndConsume("unknown")
      } yield assert(loginResult.isEmpty)
    }
    "valid login using client" in {
      for {
        loginResult <- client.login.sendAndConsume("foobar")
      } yield assert(loginResult.nonEmpty)
    }
    //#login-test-client
    //#protected-endpoint-test
    "login and access protected resource" in {
      for {
        maybeToken <- client.login.sendAndConsume("foobar")
        token = maybeToken.get
        _ = assert(token.decoded == UserInfo("Alice"))
        resource <- client.someResource.sendAndConsume(token)
      } yield assert(resource == "Hello Alice!")
    }
    //#protected-endpoint-test
  }

}
