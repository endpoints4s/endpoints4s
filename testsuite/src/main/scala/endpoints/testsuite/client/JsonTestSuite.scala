package endpoints.testsuite.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.testsuite.{Address, JsonTestApi, User}


trait JsonTestSuite[T <: JsonTestApi] extends ClientTestBase[T] {

  import io.circe.generic.auto._
  import io.circe.syntax._

  def jsonTestSuite() = {

    "Client interpreter" should {

      "return server json response" in {

        val user = User("name2", 19)
        val userStr = user.asJson.noSpaces
        val address = Address("avenue1", "NY")
        val addressStr = address.asJson.noSpaces

        wireMockServer.stubFor(post(urlEqualTo("/user")).withRequestBody(equalToJson(userStr))
          .willReturn(aResponse()
            .withStatus(200)
            .withBody(addressStr)))

        val result = call(client.jsonEndpoint, user)
        val result2 = call(client.jsonEndpointViaBuilder, user)

        result shouldEqual address
        result2 shouldEqual address

      }


    }

  }


}
