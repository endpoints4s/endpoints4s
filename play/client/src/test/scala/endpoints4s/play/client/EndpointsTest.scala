package endpoints4s.play.client

import endpoints4s.algebra.client
import endpoints4s.algebra
import endpoints4s.algebra.circe
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

import scala.concurrent.{ExecutionContext, Future}

class TestClient(address: String, wsClient: WSClient)(implicit
    EC: ExecutionContext
) extends Endpoints(address, wsClient)
    with BasicAuthentication
    with JsonEntitiesFromCodecs
    with algebra.BasicAuthenticationTestApi
    with algebra.client.ClientEndpointsTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.TextEntitiesTestApi
    with algebra.SumTypedEntitiesTestApi
    with circe.JsonFromCirceCodecTestApi
    with circe.JsonEntitiesFromCodecs

class EndpointsTest
    extends client.EndpointsTestSuite[TestClient]
    with client.BasicAuthTestSuite[TestClient]
    with client.JsonFromCodecTestSuite[TestClient]
    with client.SumTypedEntitiesTestSuite[TestClient]
    with client.TextEntitiesTestSuite[TestClient] {

  import ExecutionContext.Implicits.global

  val wsClient = new WsTestClient.InternalWSClient("http", wiremockPort)
  val client: TestClient =
    new TestClient(s"http://localhost:$wiremockPort", wsClient)

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  sumTypedRequestsTestSuite()
  textEntitiesTestSuite()

  override def afterAll(): Unit = {
    wsClient.close()
    Thread.sleep(
      6000
    ) // Unfortunate hack to let WSTestClient terminate its ActorSystem. See https://github.com/playframework/playframework/blob/8b0d5afb8c353dd8cd8d9e8057136e1858ad0173/transport/client/play-ahc-ws/src/main/scala/play/api/test/WSTestClient.scala#L142
    super.afterAll()
  }

}
