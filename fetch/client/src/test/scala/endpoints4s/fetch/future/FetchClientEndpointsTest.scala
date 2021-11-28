package endpoints4s.fetch.future

import endpoints4s.algebra
import endpoints4s.fetch.BasicAuthentication
import endpoints4s.fetch.EndpointsSettings
import endpoints4s.fetch.JsonEntitiesFromCodecs

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext

class TestClient(val settings: EndpointsSettings)(implicit val ec: ExecutionContext)
    extends Endpoints
    with BasicAuthentication
    with algebra.client.ClientEndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.TextEntitiesTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodecs
    with algebra.circe.JsonEntitiesFromCodecs

class FetchClientEndpointsTest
    extends algebra.client.EndpointsTestSuite[TestClient]
    with algebra.client.BasicAuthTestSuite[TestClient]
    with algebra.client.JsonFromCodecTestSuite[TestClient]
    with algebra.client.TextEntitiesTestSuite[TestClient]
    with algebra.client.SumTypedEntitiesTestSuite[TestClient] {

  implicit override def executionContext: ExecutionContextExecutor = JSExecutionContext.queue

  val client: TestClient = new TestClient(
    EndpointsSettings().withBaseUri(Some("http://localhost:8080"))
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  textEntitiesTestSuite()
  sumTypedRequestsTestSuite()
}
