package endpoints4s.sttp.client

import com.softwaremill.sttp
import com.softwaremill.sttp.TryHttpURLConnectionBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import endpoints4s.algebra.client.{
  BasicAuthTestSuite,
  JsonFromCodecTestSuite,
  TextEntitiesTestSuite,
  SumTypedEntitiesTestSuite,
  EndpointsTestSuite
}
import endpoints4s.algebra.{
  BasicAuthenticationTestApi,
  EndpointsTestApi,
  SumTypedEntitiesTestApi,
  TextEntitiesTestApi
}
import endpoints4s.algebra.playjson.JsonFromPlayJsonCodecTestApi

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class TestClient[R[_]](address: String, backend: sttp.SttpBackend[R, _])
    extends Endpoints(address, backend)
    with BasicAuthentication[R]
    with JsonEntitiesFromCodecs[R]
    with BasicAuthenticationTestApi
    with EndpointsTestApi
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
    new TestClient(s"http://localhost:$wiremockPort", backend)

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

  import ExecutionContext.Implicits.global

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
