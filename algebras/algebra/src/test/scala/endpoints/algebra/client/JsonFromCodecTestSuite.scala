package endpoints.algebra.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.JsonFromCodecTestApi
import endpoints.algebra.{Address, JsonFromCodecTestApi, User}

trait JsonFromCodecTestSuite[T <: JsonFromCodecTestApi] extends ClientTestBase[T] {

  def jsonFromCodecTestSuite() = {

    "Client interpreter" should {

      "encode JSON requests and decode JSON responses" in {

        val user = User("name2", 19)
        val userStr = client.jsonCodec(client.userCodec).encode(user)
        val address = Address("avenue1", "NY")
        val addressStr = client.jsonCodec(client.addressCodec).encode(address)

        wireMockServer.stubFor(
          post(urlEqualTo("/user-json-codec"))
            .withRequestBody(equalToJson(userStr))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(addressStr)
            )
        )

        whenReady(call(client.jsonCodecEndpoint, user))(_ shouldEqual address)

      }

    }

  }

}
