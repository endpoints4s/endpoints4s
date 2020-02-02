package endpoints.algebra.server

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, `WWW-Authenticate`}
import endpoints.algebra.BasicAuthenticationTestApi

trait BasicAuthenticationTestSuite[T <: BasicAuthenticationTestApi] extends EndpointsTestSuite[T] {

  "BasicAuthentication" should {

    "reject unauthenticated requests" in {
      serveEndpoint(serverApi.protectedEndpoint, Some("Hello!")) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/users")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          response.status shouldBe StatusCodes.Unauthorized
          response.header[`WWW-Authenticate`].exists(_.challenges.exists(_.scheme == "Basic")) shouldBe true
          entity shouldBe ""
          ()
        }
      }
    }

    "accept authenticated requests" in {
      serveEndpoint(serverApi.protectedEndpoint, Some("Hello!")) { port =>
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
      serveEndpoint(serverApi.protectedEndpointWithParameter, Some("Hello!")) { port =>
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
