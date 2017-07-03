package endpoints.testsuite.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.BasicAuthentication
import endpoints.testsuite.{BasicAuthTestApi, SimpleTestApi}

trait BasicAuthTestSuite[T <: BasicAuthTestApi] extends ClientTestBase[T] {

  def basicAuthSuite() = {

    "Basic auth interpreter" should {

      "authenticate with given credentials" in {

        val credentials = BasicAuthentication.Credentials("user1", "pass2")
        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo("/users"))
          .withBasicAuth(credentials.username, credentials.password)
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        val result = call(client.protectedEndpoint, credentials)

        val resultViaBuilder = call(client.protectedEndpointViaBuilder, credentials)

        result shouldEqual Some(response)
        resultViaBuilder shouldEqual Some(response)

      }

      "return None is authentication failed" in {

        val credentials = BasicAuthentication.Credentials("user1", "pass2")

        wireMockServer.stubFor(get(urlEqualTo("/users"))
          .willReturn(aResponse()
            .withStatus(403)
            .withBody("")))

        val result = call(client.protectedEndpoint, credentials)

        val resultViaBuilder = call(client.protectedEndpointViaBuilder, credentials)

        result shouldEqual None
        resultViaBuilder shouldEqual None

      }


    }

  }


}
