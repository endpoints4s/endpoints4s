package authentication

import pdi.jwt.JwtSession
import play.api.Mode
import play.api.http.{HeaderNames, Status}
import play.api.libs.ws.ahc.{AhcWSClient, AhcWSClientConfig}
import play.core.server.{NettyServer, ServerConfig}
import utest.{TestSuite, TestableSymbol, Tests, assert}

object AuthenticationTest extends TestSuite {

  val host = "0.0.0.0"
  val port = 8765
  val playConfig = ServerConfig(port = Some(port), mode = Mode.Test, address = host)
  val server = NettyServer.fromRouterWithComponents(playConfig)(new Server(_).routes).asInstanceOf[NettyServer]
  import server.materializer
  import server.actorSystem.dispatcher
  val wsClient = AhcWSClient(AhcWSClientConfig())
  val client = new Client(s"http://$host:$port", wsClient)

  def uri(path: String): String = s"http://$host:$port$path"

  override def utestAfterAll(): Unit = {
    server.stop()
    wsClient.close()
    super.utestAfterAll()
  }

  val tests = Tests {
    'unauthenticatedRequestGetsRejected - {
      for {
        response <- wsClient.url(uri("/some-resource")).get()
      } yield assert(response.status == Status.UNAUTHORIZED)
    }
    'invalidAuthenticatedRequestGetsRejected - {
      for {
        response <- wsClient.url(uri("/some-resource"))
          .withHttpHeaders(HeaderNames.AUTHORIZATION -> "lol")
          .get()
      } yield assert(response.status == Status.UNAUTHORIZED)
    }
    'invalidJsonTokenGetsRejected - {
      val token = """eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"""
      for {
        response <- wsClient.url(uri("/some-resource"))
          .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $token")
          .get()
      } yield assert(response.status == Status.UNAUTHORIZED)
    }
    'wrongLoginIsRejected - {
      for {
        loginResponse <- wsClient.url(uri("/login")).withQueryStringParameters("apiKey" -> "unknown").get()
      } yield assert(loginResponse.status == Status.BAD_REQUEST)
    }
    'loginGivesAValidJsonToken - {
      for {
        loginResponse <- wsClient.url(uri("/login")).withQueryStringParameters("apiKey" -> "foobar").get()
        token = loginResponse.headers(HeaderNames.AUTHORIZATION).head.drop("Bearer ".length)
        _ = {
          assert(loginResponse.status == Status.OK)
          val jwtSession = JwtSession.deserialize(token)
          val user = jwtSession.getAs[UserInfo]("user").get
          assert(user == UserInfo("Alice"))
        }
        response <- wsClient.url(uri("/some-resource"))
          .withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $token")
          .get()
      } yield assert(response.status == Status.OK)
    }
    //#login-test-client
    'wrongLoginUsingClient - {
      for {
        loginResult <- client.login("unknown")
      } yield assert(loginResult.isEmpty)
    }
    'validLoginUsingClient - {
      for {
        loginResult <- client.login("foobar")
      } yield assert(loginResult.nonEmpty)
    }
    //#login-test-client
    //#protected-endpoint-test
    'loginAndAccessProtectedResource - {
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
