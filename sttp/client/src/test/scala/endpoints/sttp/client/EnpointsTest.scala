package endpoints.sttp.client

import com.softwaremill.sttp
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.softwaremill.sttp.{HttpURLConnectionBackend, Id}
import endpoints.testsuite.{BasicAuthTestApi, JsonFromPlayJsonCodecTestApi, OptionalResponsesTestApi, SimpleTestApi}
import endpoints.testsuite.client.{BasicAuthTestSuite, JsonFromCodecTestSuite, OptionalResponsesTestSuite, SimpleTestSuite}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

class TestClient[R[_]](address: String, backend: sttp.SttpBackend[R, _])
                (implicit EC: ExecutionContext)
  extends Endpoints(address, backend)
    with BasicAuthentication[R]
    with OptionalResponses[R]
    with JsonEntitiesFromCodec[R]
    with BasicAuthTestApi
    with SimpleTestApi
    with OptionalResponsesTestApi
    with JsonFromPlayJsonCodecTestApi

class EndpointsTestSync
  extends SimpleTestSuite[TestClient[Id]]
    with BasicAuthTestSuite[TestClient[Id]]
    with OptionalResponsesTestSuite[TestClient[Id]]
    with JsonFromCodecTestSuite[TestClient[Id]] {

  import ExecutionContext.Implicits.global
  val backend = HttpURLConnectionBackend()

  val client: TestClient[Id] = new TestClient(s"http://localhost:$wiremockPort", backend)

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) = {
    endpoint(args) match {
      case Right(res) => Future.successful(res)
      case Left(error) => Future.failed(new Throwable(error))
    }
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

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) = {
    endpoint(args).flatMap {
      case Right(res) => Future.successful(res)
      case Left(error) => Future.failed(new Throwable(error))
    }
  }

  clientTestSuite()
  basicAuthSuite()
  optionalResponsesSuite()
  jsonFromCodecTestSuite()
}

