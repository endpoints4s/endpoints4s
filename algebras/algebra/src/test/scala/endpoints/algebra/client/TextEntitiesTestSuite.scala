package endpoints.algebra.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.TextEntitiesTestApi

trait TextEntitiesTestSuite[T <: TextEntitiesTestApi]
    extends ClientTestBase[T] {

  def textEntitiesTestSuite() = {

    "TextEntities" should {
      "produce `text/plain` requests with an explicit encoding" in {

        val utf8String = "Oekraïene"

        wireMockServer.stubFor(
          post(urlEqualTo("/text"))
            .withHeader("Content-Type", containing("text/plain"))
            .withHeader("Content-Type", containing("charset"))
            .withRequestBody(equalTo(utf8String))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(utf8String)
            )
        )

        whenReady(call(client.textRequestEndpointTest, utf8String))(
          _ shouldEqual utf8String
        )
      }
    }
  }
}
