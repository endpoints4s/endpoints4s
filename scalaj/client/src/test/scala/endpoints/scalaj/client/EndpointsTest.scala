package endpoints.scalaj.client

import endpoints.algebra.client.{BasicAuthTestSuite, EndpointsTestSuite}
import endpoints.algebra.{BasicAuthTestApi, EndpointsTestApi}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestClient(val address: String)
  extends EndpointsTestApi
  with BasicAuthTestApi
  with Endpoints
  with BasicAuthentication

class EndpointsTest
  extends EndpointsTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient] {

  val client: TestClient = new TestClient(s"localhost:$wiremockPort")

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] =
    endpoint.callAsync(args)


  clientTestSuite()

  basicAuthSuite()


}
