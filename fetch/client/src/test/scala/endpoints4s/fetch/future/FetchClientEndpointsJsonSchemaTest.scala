package endpoints4s.fetch.future

import endpoints4s.algebra.JsonTestApi
import endpoints4s.algebra.{Address, User}
import endpoints4s.fetch.BasicAuthentication
import endpoints4s.fetch.EndpointsSettings
import endpoints4s.fetch.JsonEntitiesFromSchemas
import endpoints4s.generic

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class TestJsonSchemaClient(val settings: EndpointsSettings)(implicit
    val ec: ExecutionContext
) extends Endpoints
    with BasicAuthentication
    with BasicAuthenticationTestApi
    with generic.JsonSchemas
    with JsonTestApi
    with JsonEntitiesFromSchemas {
  implicit def userCodec: JsonSchema[User] = genericJsonSchema[User]
  implicit def addresCodec: JsonSchema[Address] = genericJsonSchema[Address]
}

class FetchClientEndpointsJsonSchemaTest
    extends ClientTestBase[TestJsonSchemaClient]
    with BasicAuthTestSuite[TestJsonSchemaClient] {

  implicit override def executionContext = JSExecutionContext.queue

  val client: TestJsonSchemaClient = new TestJsonSchemaClient(
    EndpointsSettings()
      .withHost(Some("http://localhost:8080"))
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  "Client interpreter" should {

    "return server json response" in {
      val user = User("name2", 19)
      val address = Address("avenue1", "NY")

      client.jsonEndpoint(user).map(_ shouldEqual address)
    }
  }
}
