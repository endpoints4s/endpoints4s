package endpoints.scalaj.client

import endpoints.testsuite.{BasicAuthTestApi, OptionalResponsesTestApi, SimpleTestApi}
import endpoints.testsuite.client.{BasicAuthTestSuite, OptionalResponsesTestSuite, SimpleTestSuite}

class TestClient(val address: String) extends SimpleTestApi
  with BasicAuthTestApi
  with OptionalResponsesTestApi
  with Endpoints
  with BasicAuthentication
  with OptionalResponses

class EndpointsTest extends SimpleTestSuite[TestClient]
  with BasicAuthTestSuite[TestClient]
  with OptionalResponsesTestSuite[TestClient] {

  val client: TestClient = new TestClient(s"localhost:$wiremockPort")

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Resp = endpoint.callUnsafe(args)


  clientTestSuite()

  basicAuthSuite()

  optionalResponsesSuite()

}
