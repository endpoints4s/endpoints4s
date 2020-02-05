package endpoints.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import endpoints.algebra.client.{BasicAuthTestSuite, JsonTestSuite}
import endpoints.algebra.{Address, BasicAuthenticationTestApi, JsonTestApi, User}
import endpoints.generic

import scala.concurrent.{ExecutionContext, Future}

class TestJsonSchemaClient(settings: EndpointsSettings)(implicit EC: ExecutionContext,
                                              M: Materializer)
    extends Endpoints(settings)
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

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val client: TestJsonSchemaClient = new TestJsonSchemaClient(
    EndpointsSettings(
      AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost",
                                                       wiremockPort)))

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp],
                      args: Req): Future[Resp] = endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  clientTestSuite()
  basicAuthSuite()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
