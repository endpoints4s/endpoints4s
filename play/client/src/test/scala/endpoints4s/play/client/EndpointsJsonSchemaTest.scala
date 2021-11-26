package endpoints4s.play.client

import endpoints4s.algebra
import endpoints4s.algebra.{Address, User, client}
import endpoints4s.generic
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient

import scala.concurrent.{ExecutionContext, Future}

class TestJsonSchemaClient(address: String, wsClient: WSClient)(implicit
    EC: ExecutionContext
) extends Endpoints(address, wsClient)
    with BasicAuthentication
    with algebra.BasicAuthenticationTestApi
    with generic.JsonSchemas
    with algebra.JsonTestApi
    with JsonEntitiesFromSchemas {
  implicit def userCodec: JsonSchema[User] = genericJsonSchema[User]
  implicit def addresCodec: JsonSchema[Address] = genericJsonSchema[Address]
}

class EndpointsJsonSchemaTest extends client.JsonTestSuite[TestJsonSchemaClient] {

  val wsClient = new WsTestClient.InternalWSClient("http", stubServerPort)
  val client: TestJsonSchemaClient =
    new TestJsonSchemaClient(s"http://localhost:$stubServerPort", wsClient)

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  clientTestSuite()

  override def afterAll(): Unit = {
    wsClient.close()
    Thread.sleep(
      6000
    ) // Unfortunate hack to let WSTestClient terminate its ActorSystem. See https://github.com/playframework/playframework/blob/8b0d5afb8c353dd8cd8d9e8057136e1858ad0173/transport/client/play-ahc-ws/src/main/scala/play/api/test/WSTestClient.scala#L142
    super.afterAll()
  }
}
