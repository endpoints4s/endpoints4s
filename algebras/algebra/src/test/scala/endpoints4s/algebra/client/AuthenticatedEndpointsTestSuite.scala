package endpoints4s.algebra.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints4s.algebra.{AuthenticatedEndpointsTestApi, AuthenticatedEndpointsClient}
import endpoints4s.algebra.AuthenticatedEndpoints._
import java.util.Base64

trait AuthenticatedEndpointsTestSuite[
    T <: AuthenticatedEndpointsTestApi with AuthenticatedEndpointsClient
] extends ClientTestBase[T] {
  def authTestSuite() = {

    "Client interpreter" should {

      "authenticate with given username/password" in {

        val credentials = Credentials("user1", "pass2")
        val response = "wiremockeResponse"

        wireMockServer.stubFor(
          get(urlEqualTo("/users"))
            .withBasicAuth(credentials.username, credentials.password)
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(response)
            )
        )

        whenReady(call(client.basicAuthEndpoint, credentials))(
          _ shouldEqual Some(response)
        )

      }

      "return None if not authenticated" in {

        val credentials = Credentials("user1", "pass2")

        wireMockServer.stubFor(
          get(urlEqualTo("/users"))
            .willReturn(
              aResponse()
                .withStatus(401)
                .withBody("")
            )
        )

        whenReady(call(client.basicAuthEndpoint, credentials))(
          _ shouldEqual None
        )

      }

      "authenticate with given token" in {

        val credentials = Bearer("token")
        val response = "wiremockeResponse"

        wireMockServer.stubFor(
          get(urlEqualTo("/users"))
            .withHeader(
              "Authorization",
              equalTo(s"Bearer ${Base64.getEncoder().encodeToString("token".getBytes())}")
            )
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(response)
            )
        )

        whenReady(call(client.bearerAuthEndpoint, credentials))(
          _ shouldEqual Some(response)
        )

      }
    }
  }
}
