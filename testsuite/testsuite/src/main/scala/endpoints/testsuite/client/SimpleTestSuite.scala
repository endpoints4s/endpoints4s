package endpoints.testsuite.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.testsuite.SimpleTestApi

trait SimpleTestSuite[T <: SimpleTestApi] extends ClientTestBase[T] {

  def clientTestSuite() = {

    "Client interpreter" should {

      "return server response" in {

        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo("/user/userId/description?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        whenReady(call(client.smokeEndpoint, ("userId", "name1", 18))) { _ shouldEqual response }
        whenReady(call(client.emptyResponseSmokeEndpoint, ("userId", "name1", 18))) { _ shouldEqual(()) }

      }

      "throw exception when 5xx is returned from server" in {
        wireMockServer.stubFor(get(urlEqualTo("/user/userId/description?name=name1&age=18"))
          .willReturn(aResponse()
            .withStatus(501)
            .withBody("")))

        whenReady(call(client.smokeEndpoint, ("userId", "name1", 18)).failed)(x => x.getMessage shouldBe "Unexpected status code: 501")
        whenReady(call(client.emptyResponseSmokeEndpoint, ("userId", "name1", 18)).failed)(x => x.getMessage shouldBe "Unexpected status code: 501")
      }



    }

  }


}
