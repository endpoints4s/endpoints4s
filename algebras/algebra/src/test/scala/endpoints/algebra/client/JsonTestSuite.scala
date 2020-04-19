package endpoints.algebra.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.{Address, JsonTestApi, User}

trait JsonTestSuite[T <: JsonTestApi] extends ClientTestBase[T] {

  def clientTestSuite() = {

    "Client interpreter" should {

      "return server json response" in {

        val user = User("name2", 19)
        val userStr = """{"name":"name2","age":19}"""
        val address = Address("avenue1", "NY")
        val addressStr = """{"street":"avenue1","city":"NY"}"""
        wireMockServer.stubFor(
          post(urlEqualTo("/user"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(equalToJson(userStr))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(addressStr)
            )
        )

        whenReady(call(client.jsonEndpoint, user))(_ shouldEqual address)

      }

    }

  }

}
