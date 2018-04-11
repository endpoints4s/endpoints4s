package endpoints.testsuite.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.testsuite.OptionalResponsesTestApi

trait OptionalResponsesTestSuite[T <: OptionalResponsesTestApi] extends ClientTestBase[T] {

  def optionalResponsesSuite() = {

    "Client interpreter of optional responses" should {

      "return Some when response code is 2xx" in {

        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo("/users/1"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.optionalEndpoint, ()))(_ shouldEqual Some(response))

      }

      "return None if server returned 404" in {

        wireMockServer.stubFor(get(urlEqualTo("/users/1"))
          .willReturn(aResponse()
            .withStatus(404)
            .withBody("")))

        whenReady(call(client.optionalEndpoint, ()))(_ shouldEqual None)

      }
    }
  }
}
