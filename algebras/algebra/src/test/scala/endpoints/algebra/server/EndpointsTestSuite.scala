package endpoints.algebra.server
import com.softwaremill.sttp.quick._
import endpoints.algebra.EndpointsTestApi

trait EndpointsTestSuite[T <: EndpointsTestApi] extends ServerTestBase[T] {

  def serverTestSuite() = {

    "Server interpreter" should {

      "return server response" in {

        val mockedResponse = "interpretedServerResponse"

        val server = serveEndpoint(serverApi.smokeEndpoint, mockedResponse)
        server.start()

        val response  = sttp.get(uri"http://localhost:${server.port}/user/userId/description?name=name1&age=18").send()
        assert(response.body.isRight)
        assert(response.body.right.get == mockedResponse)
        assert(response.code == 200)

        server.stop()
      }
    }
  }


}
