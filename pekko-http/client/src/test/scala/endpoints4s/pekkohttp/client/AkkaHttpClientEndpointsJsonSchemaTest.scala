package endpoints4s.pekkohttp.client

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.testkit.TestKit
import endpoints4s.algebra.client.{BasicAuthTestSuite, JsonTestSuite}
import endpoints4s.algebra.{Address, BasicAuthenticationTestApi, JsonTestApi, User}
import endpoints4s.generic

import scala.concurrent.{ExecutionContext, Future}

class TestJsonSchemaClient(settings: EndpointsSettings)(implicit
    EC: ExecutionContext,
    M: Materializer
) extends Endpoints(settings)
    with BasicAuthentication
    with BasicAuthenticationTestApi
    with generic.JsonSchemas
    with JsonTestApi
    with JsonEntitiesFromSchemas {
  implicit def userCodec: JsonSchema[User] = genericJsonSchema[User]
  implicit def addresCodec: JsonSchema[Address] = genericJsonSchema[Address]
}

class AkkaHttpClientEndpointsJsonSchemaTest
    extends JsonTestSuite[TestJsonSchemaClient]
    with BasicAuthTestSuite[TestJsonSchemaClient] {

  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  val client: TestJsonSchemaClient = new TestJsonSchemaClient(
    EndpointsSettings(
      PekkoHttpRequestExecutor
        .cachedHostConnectionPool("localhost", stubServerPortHTTP)
    )
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
