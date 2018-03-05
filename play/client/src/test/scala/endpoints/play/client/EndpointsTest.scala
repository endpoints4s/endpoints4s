package endpoints.play.client

import endpoints.testsuite.{BasicAuthTestApi, OptionalResponsesTestApi, SimpleTestApi}
import endpoints.testsuite.client.{BasicAuthTestSuite, OptionalResponsesTestSuite, SimpleTestSuite}
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

import scala.concurrent.{ExecutionContext, Future}

class TestClient(address: String, wsClient: WSClient)
  (implicit EC: ExecutionContext)
  extends Endpoints(address, wsClient)
    with OptionalResponses
    with BasicAuthTestApi
    with SimpleTestApi
    with OptionalResponsesTestApi
    with BasicAuthentication

class EndpointsTest
  extends SimpleTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient]
    with OptionalResponsesTestSuite[TestClient] {

  import ExecutionContext.Implicits.global

  val client: TestClient = WsTestClient.withClient(c => {
    new TestClient(s"http://localhost:$wiremockPort", c)
  })

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  optionalResponsesSuite()
}

