package endpoints.algebra.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.{Address, JsonFromCodecTestApi, User}

trait JsonFromCodecTestSuite extends ClientTestBase {

  val api: JsonFromCodecTestApi

  override type Endpoint[Req, Resp] = api.entities.endpoints.Endpoint[Req, Resp]

  def jsonFromCodecTestSuite() = {

    "Client interpreter" should {

      "encode JSON requests and decode JSON responses" in {

        val user = User("name2", 19)
        val userStr = api.entities.jsonCodec(api.userCodec).encode(user)
        val address = Address("avenue1", "NY")
        val addressStr = api.entities.jsonCodec(api.addressCodec).encode(address)

        wireMockServer.stubFor(
          post(urlEqualTo("/user-json-codec"))
            .withRequestBody(equalToJson(userStr))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(addressStr)
            )
        )

        whenReady(call(api.jsonCodecEndpoint, user))(_ shouldEqual address)

      }

    }

  }

}
