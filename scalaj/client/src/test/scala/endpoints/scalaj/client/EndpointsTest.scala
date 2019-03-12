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

  def encodeUrl[A](url: client.Url[A])(a: A): String = {
    val req = url.toReq(a)
    val encodedUrl = req.urlBuilder(req)
    val pathAndQuery =
      encodedUrl.drop(s"http://localhost:$wiremockPort".size) // Remove scheme, host and port from URL
    if (pathAndQuery.startsWith("/?") || pathAndQuery == "/") pathAndQuery.drop(1) // For some reason, scalaj always inserts a slash when the path is empty
    else pathAndQuery
  }

  clientTestSuite()

  basicAuthSuite()


}
