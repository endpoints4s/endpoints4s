package endpoints4s.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import endpoints4s.algebra.client.{
  AuthenticatedEndpointsTestSuite,
  BasicAuthTestSuite,
  JsonTestSuite
}
import endpoints4s.algebra.{
  Address,
  AuthenticatedEndpointsTestApi,
  AuthenticatedEndpointsClient,
  BasicAuthenticationTestApi,
  JsonTestApi,
  User
}
import endpoints4s.generic

import scala.concurrent.{ExecutionContext, Future}
import scala.annotation.nowarn

@nowarn("cat=deprecation")
class TestJsonSchemaClient(settings: EndpointsSettings)(implicit
    EC: ExecutionContext,
    M: Materializer
) extends Endpoints(settings)
    with BasicAuthentication
    with BasicAuthenticationTestApi
    with AuthenticatedEndpointsTestApi
    with AuthenticatedEndpointsClient
    with generic.JsonSchemas
    with JsonTestApi
    with JsonEntitiesFromSchemas {
  implicit def userCodec: JsonSchema[User] = genericJsonSchema[User]
  implicit def addresCodec: JsonSchema[Address] = genericJsonSchema[Address]
}

class AkkaHttpClientEndpointsJsonSchemaTest
    extends JsonTestSuite[TestJsonSchemaClient]
    with AuthenticatedEndpointsTestSuite[TestJsonSchemaClient]
    with BasicAuthTestSuite[TestJsonSchemaClient] {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val client: TestJsonSchemaClient = new TestJsonSchemaClient(
    EndpointsSettings(
      AkkaHttpRequestExecutor
        .cachedHostConnectionPool("localhost", wiremockPort)
    )
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  authTestSuite()
  clientTestSuite()
  basicAuthSuite()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
