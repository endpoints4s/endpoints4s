package endpoints.play.client

import endpoints.algebra.client
import endpoints.algebra
import endpoints.algebra.circe
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

import scala.concurrent.{ExecutionContext, Future}


class TestClient(address: String, wsClient: WSClient)
  (implicit EC: ExecutionContext)
  extends Endpoints(address, wsClient)
    with BasicAuthentication
    with JsonEntitiesFromCodec
    with algebra.BasicAuthTestApi
    with algebra.EndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with circe.JsonFromCirceCodecTestApi
    with circe.JsonEntitiesFromCodec

class EndpointsTest
  extends client.EndpointsTestSuite[TestClient]
    with client.BasicAuthTestSuite[TestClient]
    with client.JsonFromCodecTestSuite[TestClient]
{

  import ExecutionContext.Implicits.global

  val client: TestClient = WsTestClient.withClient(c => {
    new TestClient(s"http://localhost:$wiremockPort", c)
  })

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
}

