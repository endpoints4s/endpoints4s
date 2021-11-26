package endpoints4s.scalaj.client

import endpoints4s.algebra.client.{BasicAuthTestSuite, EndpointsTestSuite, ClientEndpointsTestApi}
import endpoints4s.algebra.BasicAuthenticationTestApi

import scala.concurrent.Future

class TestClient(val address: String)
    extends ClientEndpointsTestApi
    with BasicAuthenticationTestApi
    with Endpoints
    with BasicAuthentication

class EndpointsTest extends EndpointsTestSuite[TestClient] with BasicAuthTestSuite[TestClient] {

  val client: TestClient = new TestClient(s"localhost:$stubServerPort")

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] =
    endpoint.callAsync(args)

  clientTestSuite()

  basicAuthSuite()

}
