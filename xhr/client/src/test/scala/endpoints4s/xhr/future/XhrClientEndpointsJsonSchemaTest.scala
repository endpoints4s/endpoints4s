package endpoints4s.xhr.future

import endpoints4s.algebra.Address
import endpoints4s.algebra.BasicAuthenticationTestApi
import endpoints4s.algebra.JsonTestApi
import endpoints4s.algebra.User
import endpoints4s.algebra.client.BasicAuthTestSuite
import endpoints4s.algebra.client.JsonTestSuite
import endpoints4s.generic
import endpoints4s.xhr.BasicAuthentication
import endpoints4s.xhr.EndpointsSettings
import endpoints4s.xhr.JsonEntitiesFromSchemas

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class TestJsonSchemaClient(val settings: EndpointsSettings)
    extends Endpoints
    with BasicAuthentication
    with BasicAuthenticationTestApi
    with generic.JsonSchemas
    with JsonTestApi
    with JsonEntitiesFromSchemas {
  implicit def userCodec: JsonSchema[User] = genericJsonSchema[User]
  implicit def addresCodec: JsonSchema[Address] = genericJsonSchema[Address]
}

class XhrClientEndpointsJsonSchemaTest
    extends JsonTestSuite[TestJsonSchemaClient]
    with BasicAuthTestSuite[TestJsonSchemaClient] {

  implicit override def executionContext: ExecutionContextExecutor = JSExecutionContext.queue

  val client: TestJsonSchemaClient = new TestJsonSchemaClient(
    EndpointsSettings().withBaseUri(Some(s"http://localhost:$stubServerPort"))
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args).future
}
