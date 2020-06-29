package endpoints4s.algebra.client

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints4s.algebra

trait SumTypedEntitiesTestSuite[
    T <: algebra.SumTypedEntitiesTestApi
] extends ClientTestBase[T] {

  def sumTypedRequestsTestSuite() = {

    "Client interpreter" should {

      "handle the sum-typed request entities" in {
        val user = algebra.User("name2", 19)
        val name = "name3"

        wireMockServer.stubFor(
          post(urlEqualTo("/user-or-name"))
            .withHeader(
              "Content-Type",
              matching("application/json|(text/plain.*)")
            )
            .willReturn(
              aResponse().withStatus(200)
            )
        )

        whenReady(call(client.sumTypedEndpoint2, Left(user)))(_.shouldEqual(()))
        whenReady(call(client.sumTypedEndpoint2, Right(name)))(
          _.shouldEqual(())
        )
      }
    }
  }

}
