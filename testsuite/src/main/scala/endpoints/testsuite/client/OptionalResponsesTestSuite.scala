package endpoints.testsuite.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.BasicAuthentication
import endpoints.testsuite.{BasicAuthTestApi, OptionalResponsesTestApi}

trait OptionalResponsesTestSuite[T <: OptionalResponsesTestApi] extends ClientTestBase[T] {

  def optionalResponsesSuite() = {

    "Client interpreter" should {

      "return the content if present" in {

        val credentials = BasicAuthentication.Credentials("user1", "pass2")
        val response = "wiremockeResponse"

        wireMockServer.stubFor(get(urlEqualTo("/users"))
          .withBasicAuth(credentials.username, credentials.password)
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(response)))

        val result = call(client.optionalResponseEndp, ())
        val result2 = call(client.optionalResponseEndpViaBuilder, ())
        val result3 = call(client.optionalResponseEndpViaBuilder2, ())


        result shouldEqual Some(response)
        result2 shouldEqual Some(response)
        result3 shouldEqual Some(response)

      }

      "return None in case of 404" in {

        val credentials = BasicAuthentication.Credentials("user1", "pass2")

        wireMockServer.stubFor(get(urlEqualTo("/users"))
          .willReturn(aResponse()
            .withStatus(404)
            .withBody("")))

        val result = call(client.optionalResponseEndp, ())
        val result2 = call(client.optionalResponseEndpViaBuilder, ())
        val result3 = call(client.optionalResponseEndpViaBuilder2, ())

        result shouldEqual None
        result2 shouldEqual None
        result3 shouldEqual None

      }


    }

  }


}
