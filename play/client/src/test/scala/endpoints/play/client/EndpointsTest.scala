package endpoints.play.client

import endpoints.testsuite._
import endpoints.testsuite.client.{BasicAuthTestSuite, JsonFromCodecTestSuite, OptionalResponsesTestSuite, SimpleTestSuite}
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

import scala.concurrent.{ExecutionContext, Future}

class TestClient(address: String, wsClient: WSClient)
  (implicit EC: ExecutionContext)
  extends Endpoints(address, wsClient)
    with BasicAuthentication
    with OptionalResponses
    with JsonEntitiesFromCodec
    with BasicAuthTestApi
    with SimpleTestApi
    with OptionalResponsesTestApi
    with JsonFromPlayJsonCodecTestApi

class EndpointsTest
  extends SimpleTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient]
    with OptionalResponsesTestSuite[TestClient]
    with JsonFromCodecTestSuite[TestClient] {

  import ExecutionContext.Implicits.global

  val client: TestClient = WsTestClient.withClient(c => {
    new TestClient(s"http://localhost:$wiremockPort", c)
  })

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  optionalResponsesSuite()
  jsonFromCodecTestSuite()
}

