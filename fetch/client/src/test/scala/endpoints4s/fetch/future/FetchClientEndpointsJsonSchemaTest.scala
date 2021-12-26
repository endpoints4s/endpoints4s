package endpoints4s.fetch.future

import endpoints4s.algebra.Address
import endpoints4s.algebra.JsonTestApi
import endpoints4s.algebra.User
import endpoints4s.algebra.client.JsonTestSuite
import endpoints4s.fetch.EndpointsSettings
import endpoints4s.fetch.JsonEntitiesFromSchemas
import endpoints4s.generic

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class TestJsonSchemaClient(val settings: EndpointsSettings)
    extends Endpoints
    with generic.JsonSchemas
    with JsonTestApi
    with JsonEntitiesFromSchemas {
  implicit def userCodec: JsonSchema[User] = genericJsonSchema[User]
  implicit def addresCodec: JsonSchema[Address] = genericJsonSchema[Address]
}

class FetchClientEndpointsJsonSchemaTest extends JsonTestSuite[TestJsonSchemaClient] {

  implicit override def executionContext: ExecutionContextExecutor = JSExecutionContext.queue

  val client: TestJsonSchemaClient = new TestJsonSchemaClient(
    EndpointsSettings()
      .withBaseUri(Some("http://localhost:8080"))
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args).future

  clientTestSuite()
}
