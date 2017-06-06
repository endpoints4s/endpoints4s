package endpoints.scalaj.client

import endpoints.testsuite.SimpleTestApi
import endpoints.testsuite.client.SimpleTestSuite

class TestClient(val address: String) extends SimpleTestApi with Endpoints

class EndpointsTest extends SimpleTestSuite[TestClient] {

  override val client: TestClient = new TestClient(s"localhost:$wiremockPort")

  override def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Resp = endpoint.callUnsafe(args)


  clientTestSuite()

}
