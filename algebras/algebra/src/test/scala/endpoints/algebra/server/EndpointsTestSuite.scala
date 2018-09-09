package endpoints.algebra.server

import java.time.LocalDate
import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import endpoints.algebra.EndpointsTestApi

trait EndpointsTestSuite[T <: EndpointsTestApi] extends ServerTestBase[T] {

  def serverTestSuite() = {

    "Server interpreter" should {

      "return server response" in {

        val mockedResponse = "wiremockeResponse"

        val server = serveEndpoint(serverApi.smokeEndpoint, mockedResponse)

        val response  = sttp.get("/user/userId/description?name=name1&age=18")
        assert(response.body == mockedResponse)
        assert(response.code == ok)
        server.stop()

      }
    }
  }


}
