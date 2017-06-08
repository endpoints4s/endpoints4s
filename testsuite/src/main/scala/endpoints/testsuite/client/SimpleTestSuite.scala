package endpoints.testsuite.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import endpoints.algebra
import endpoints.testsuite.SimpleTestApi
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpec}

trait SimpleTestSuite[T <: SimpleTestApi] extends ClientTestBase[T] {

  def clientTestSuite() = {

    "Client interpreter" should {

      "return server response" in {

        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo("/user/userId/description?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        val result = call(client.smokeEndpoint, ("userId", "name1", 18))
        val result2 = call(client.smokeEndpointViaBuilder, ("userId", "name1", 18))

        result shouldEqual response
        result2 shouldEqual response

      }

      "throw exception when 5xx is returned from server" in {

        wireMockServer.stubFor(get(urlEqualTo("/user/userId/description?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(501)
            .withBody("")))

        a[Exception] should be thrownBy {
          call(client.smokeEndpoint, ("userId", "name1", 18))
        }
        a[Exception] should be thrownBy {
          call(client.smokeEndpointViaBuilder, ("userId", "name1", 18))
        }

      }


    }

  }


}
