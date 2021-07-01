package endpoints4s.algebra.server

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, `WWW-Authenticate`}
import endpoints4s.algebra.AuthenticatedEndpointsTestApi

trait AuthenticatedEndpointsTestSuite[T <: AuthenticatedEndpointsTestApi]
    extends EndpointsTestSuite[T] {

  "Authentication" should {

    "reject unauthenticated requests" in {
      serveEndpoint(serverApi.basicAuthEndpoint, None) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/users")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          response.status shouldBe StatusCodes.Unauthorized
          response
            .header[`WWW-Authenticate`]
            .exists(_.challenges.exists(_.scheme == "Basic")) shouldBe true
          entity shouldBe ""
          ()
        }
      }
    }

    "reject unauthenticated requests with real" in {
      serveEndpoint(serverApi.basicAuthEndpointWithRealm, None) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/users")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          response.status shouldBe StatusCodes.Unauthorized
          response
            .header[`WWW-Authenticate`]
            .exists(
              _.challenges.exists(challenge =>
                challenge.scheme == "Basic" && challenge.realm == "Test API"
              )
            ) shouldBe true
          entity shouldBe ""
          ()
        }
      }
    }

    "accept authenticated requests" in {
      serveEndpoint(serverApi.basicAuthEndpoint, Some("Hello!")) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/users")
            .addCredentials(BasicHttpCredentials("admin", "foo"))
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          response.status shouldBe StatusCodes.OK
          entity shouldBe "Hello!"
          ()
        }
      }
    }

    "reject unauthenticated requests with invalid parameters before handling authorization" in {
      serveEndpoint(serverApi.basicAuthEndpointWithParameter, Some("Hello!")) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/users/foo")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          response.status shouldBe StatusCodes.BadRequest
          entity shouldBe "[\"Invalid integer value 'foo' for segment 'id'\"]"
          ()
        }
      }
    }

  }

}
