package endpoints.scalaj.client

import endpoints.algebra
import endpoints.algebra.client.{BasicAuthTestSuite, EndpointsTestSuite}
import endpoints.algebra.{BasicAuthTestApi, EndpointsTestApi}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestClient(val _address: String)
  extends EndpointsTestApi
  with BasicAuthTestApi {
  override val endpoints: Endpoints = new Endpoints {
    override val requests: Requests = new Requests {
      override val urls: Urls = new Urls {
        override def address: String = _address
      }
      override val methods: Methods = new Methods {}
    }
    override val responses: Responses = new Responses {}
  }
  override val basicAuth: algebra.BasicAuthentication = new BasicAuthentication {
    override val endpoints: Endpoints = TestClient.this.endpoints
  }
}

class EndpointsTest {


  val endpointsSuite = new EndpointsTestSuite {
    val api: TestClient = new TestClient(s"localhost:$wiremockPort")

    def call[Req, Resp](endpoint: Endpoint[Req, Resp], args: Req): Future[Resp] =
      endpoint.asInstanceOf[api.endpoints.Endpoint[Req, Resp]].callAsync(args)
  }
  val  basicAuthSuite = new BasicAuthTestSuite {
    val api: TestClient = new TestClient(s"localhost:$wiremockPort")

    def call[Req, Resp](endpoint: Endpoint[Req, Resp], args: Req): Future[Resp] =
      endpoint.asInstanceOf[api.endpoints.Endpoint[Req, Resp]].callAsync(args)
  }

  endpointsSuite.clientTestSuite()

  basicAuthSuite.basicAuthSuite()


}
