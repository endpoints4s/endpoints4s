package endpoints.play.client

import endpoints.algebra.client.{BasicAuthTestSuite, EndpointsTestSuite}
import endpoints.algebra.{BasicAuthTestApi, EndpointsTestApi}
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

import scala.concurrent.{ExecutionContext, Future}

//TODO fix json tests
class TestClient(address: String, wsClient: WSClient)
  (implicit EC: ExecutionContext)
  extends Endpoints(address, wsClient)
    with BasicAuthentication
//    with JsonEntitiesFromCodec
    with BasicAuthTestApi
    with EndpointsTestApi
//    with JsonFromPlayJsonCodecTestApi

class EndpointsTest
  extends EndpointsTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient]
//    with JsonFromCodecTestSuite[TestClient]
{

  import ExecutionContext.Implicits.global

  val client: TestClient = WsTestClient.withClient(c => {
    new TestClient(s"http://localhost:$wiremockPort", c)
  })

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
//  jsonFromCodecTestSuite()
}

