package endpoints.algebra.client
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, equalToJson, get, post, urlEqualTo}
import endpoints.algebra.User
import endpoints.algebra.utils.CrudEndpointsTestApi

trait CrudEndpointsTestSuite[T <: CrudEndpointsTestApi] extends ClientTestBase[T] {

  def crudEndpointTestSuite() = {
    "Client interpreter" should {
      val userId       = "1"
      val basePath     = "user"
      val crudEndpoint = client.crud(basePath)
      val user         = User("name2", 19)
      val userStr      = """{"name":"name2","age":19}"""
      val usersStr     = """[{"name":"name2","age":19}]"""

      "create user based on CRUD create endpoint" in {

        wireMockServer.stubFor(
          post(urlEqualTo(s"/$basePath"))
            .withRequestBody(equalToJson(userStr))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(userStr)
            )
        )

        whenReady(call(crudEndpoint.create, user)) {
          _ shouldEqual user
        }
      }

      "return user from server based on CRUD getById endpoint" in {

        wireMockServer.stubFor(
          get(urlEqualTo(s"/$basePath/$userId"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(userStr)
            )
        )

        whenReady(call(crudEndpoint.getById, userId)) {
          _ shouldEqual Some(user)
        }
      }

      "return all users from server based on CRUD getAll endpoint" in {

        wireMockServer.stubFor(
          get(urlEqualTo(s"/$basePath"))
            .willReturn(
              aResponse()
                .withStatus(200)
                .withBody(usersStr)
            )
        )

        whenReady(call(crudEndpoint.getAll, ())) {
          _ shouldEqual Seq(user)
        }
      }

      "delete user based on CRUD delete endpoint" in {

        wireMockServer.stubFor(
          delete(urlEqualTo(s"/$basePath/$userId"))
            .willReturn(
              aResponse()
                .withStatus(200)
            )
        )

        whenReady(call(crudEndpoint.delete, userId)) {
          _ shouldEqual Some(())
        }
      }
    }
  }
}
