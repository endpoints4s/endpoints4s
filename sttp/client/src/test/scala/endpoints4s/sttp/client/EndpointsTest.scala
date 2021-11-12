package endpoints4s.sttp.client

import _root_.sttp.client3.{SttpBackend, TryHttpURLConnectionBackend}
import _root_.sttp.client3.akkahttp.AkkaHttpBackend
import endpoints4s.algebra.client.{
  BasicAuthTestSuite,
  EndpointsTestSuite,
  JsonFromCodecTestSuite,
  SumTypedEntitiesTestSuite,
  TextEntitiesTestSuite,
  ClientEndpointsTestApi
}
import endpoints4s.algebra.{
  BasicAuthenticationTestApi,
  SumTypedEntitiesTestApi,
  TextEntitiesTestApi
}
import endpoints4s.algebra.playjson.JsonFromPlayJsonCodecTestApi

import scala.concurrent.Future
import scala.util.Try

class TestClient[R[_]](address: String, backend: SttpBackend[R, Any])
    extends Endpoints(address, backend)
    with BasicAuthentication[R]
    with JsonEntitiesFromCodecs[R]
    with BasicAuthenticationTestApi
    with ClientEndpointsTestApi
    with JsonFromPlayJsonCodecTestApi
    with SumTypedEntitiesTestApi
    with TextEntitiesTestApi

class EndpointsTestSync
    extends EndpointsTestSuite[TestClient[Try]]
    with BasicAuthTestSuite[TestClient[Try]]
    with JsonFromCodecTestSuite[TestClient[Try]]
    with SumTypedEntitiesTestSuite[TestClient[Try]]
    with TextEntitiesTestSuite[TestClient[Try]] {

  val backend = TryHttpURLConnectionBackend()

  val client: TestClient[Try] =
    new TestClient[Try](s"http://localhost:$wiremockPort", backend)

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) = {
    Future.fromTry(endpoint(args))
  }

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  sumTypedRequestsTestSuite()
  textEntitiesTestSuite()
}

class EndpointsTestAkka
    extends EndpointsTestSuite[TestClient[Future]]
    with BasicAuthTestSuite[TestClient[Future]]
    with JsonFromCodecTestSuite[TestClient[Future]]
    with SumTypedEntitiesTestSuite[TestClient[Future]]
    with TextEntitiesTestSuite[TestClient[Future]] {

  val backend = AkkaHttpBackend()

  val client: TestClient[Future] =
    new TestClient(s"http://localhost:$wiremockPort", backend)

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) =
    endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  sumTypedRequestsTestSuite()
  textEntitiesTestSuite()

  override def afterAll(): Unit = {
    backend.close()
    Thread.sleep(1000) // See https://github.com/softwaremill/sttp/issues/269
    super.afterAll()
  }
}
