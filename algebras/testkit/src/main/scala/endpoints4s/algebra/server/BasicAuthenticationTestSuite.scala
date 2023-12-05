package endpoints4s.algebra.server

import org.apache.pekko.http.scaladsl.model.{HttpRequest, StatusCodes}
import org.apache.pekko.http.scaladsl.model.headers.{BasicHttpCredentials, `WWW-Authenticate`}
import endpoints4s.algebra.BasicAuthenticationTestApi

trait BasicAuthenticationTestSuite[T <: BasicAuthenticationTestApi] extends EndpointsTestSuite[T] {

  "BasicAuthentication" should {

    "reject unauthenticated requests" in {
      serveEndpoint(serverApi.protectedEndpoint, Some("Hello!")) { port =>
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

    "reject authenticated requests with invalid parameters" in {
      serveEndpoint(serverApi.protectedEndpointWithParameter, Some("Hello!")) { port =>
        val request =
          HttpRequest(uri = s"http://localhost:$port/users/foo")
            .addCredentials(BasicHttpCredentials("admin", "foo"))
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          response.status shouldBe StatusCodes.BadRequest
          entity shouldBe "[\"Invalid integer value 'foo' for segment 'id'\"]"
          ()
        }
      }
    }

    "reject unauthenticated requests with Unauthorized response before validating query parameters" in {
      serveEndpoint(serverApi.protectedEndpointWithParameter, Some("Hello!")) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/users/foo")
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

    "fall through to another endpoint if url doesn't match" in {
      serveManyEndpoints(
        EndpointImplementation(
          serverApi.protectedEndpointWithParameter,
          (_: Any) => Some("Protected")
        ),
        EndpointImplementation(serverApi.unprotectedEndpoint, (_: Any) => "Unprotected")
      ) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/users")
        whenReady(sendAndDecodeEntityAsText(request)) { case (response, entity) =>
          response.status shouldBe StatusCodes.OK
          entity shouldBe "Unprotected"
          ()
        }
      }
    }

  }

}
