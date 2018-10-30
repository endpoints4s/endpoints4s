package endpoints.algebra.server
import com.softwaremill.sttp.quick._
import endpoints.algebra.EndpointsTestApi

trait EndpointsTestSuite[T <: EndpointsTestApi] extends ServerTestBase[T] {

  def serverTestSuite() = {

    "Server interpreter" should {

      "return server response" in {

        val mockedResponse = "interpretedServerResponse"

        serveEndpoint(serverApi.smokeEndpoint, mockedResponse) { port =>
          val response  = sttp.get(uri"http://localhost:$port/user/userId/description?name=name1&age=18").send()
          assert(response.body.isRight)
          assert(response.body.right.get == mockedResponse)
          assert(response.code == 200)
        }
      }
    }
  }


}
