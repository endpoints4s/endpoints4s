package endpoints.sttp.client

import com.softwaremill.sttp
import com.softwaremill.sttp.TryHttpURLConnectionBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import endpoints.testsuite.client.{BasicAuthTestSuite, JsonFromCodecTestSuite, OptionalResponsesTestSuite, SimpleTestSuite}
import endpoints.testsuite.{BasicAuthTestApi, JsonFromPlayJsonCodecTestApi, OptionalResponsesTestApi, SimpleTestApi}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

class TestClient[R[_]](address: String, backend: sttp.SttpBackend[R, _])
  extends Endpoints(address, backend)
    with BasicAuthentication[R]
    with OptionalResponses[R]
    with JsonEntitiesFromCodec[R]
    with BasicAuthTestApi
    with SimpleTestApi
    with OptionalResponsesTestApi
    with JsonFromPlayJsonCodecTestApi

class EndpointsTestSync
  extends SimpleTestSuite[TestClient[Try]]
    with BasicAuthTestSuite[TestClient[Try]]
    with OptionalResponsesTestSuite[TestClient[Try]]
    with JsonFromCodecTestSuite[TestClient[Try]] {

  val backend = TryHttpURLConnectionBackend()

  val client: TestClient[Try] = new TestClient(s"http://localhost:$wiremockPort", backend)

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) = {
    Future.fromTry(endpoint(args))
  }

  clientTestSuite()
  basicAuthSuite()
  optionalResponsesSuite()
  jsonFromCodecTestSuite()
}

class EndpointsTestAkka
  extends SimpleTestSuite[TestClient[Future]]
    with BasicAuthTestSuite[TestClient[Future]]
    with OptionalResponsesTestSuite[TestClient[Future]]
    with JsonFromCodecTestSuite[TestClient[Future]] {

  import ExecutionContext.Implicits.global
  val backend = AkkaHttpBackend()

  val client: TestClient[Future] = new TestClient(s"http://localhost:$wiremockPort", backend)

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  optionalResponsesSuite()
  jsonFromCodecTestSuite()
}

