package endpoints.sttp.client

import com.softwaremill.sttp
import com.softwaremill.sttp.TryHttpURLConnectionBackend
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import endpoints.algebra.client.{BasicAuthTestSuite, JsonFromCodecTestSuite, EndpointsTestSuite}
import endpoints.algebra.{BasicAuthTestApi, EndpointsTestApi}
import endpoints.algebra.playjson.JsonFromPlayJsonCodecTestApi

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

class TestClient[R[_]](address: String, backend: sttp.SttpBackend[R, _])
  extends Endpoints(address, backend)
    with BasicAuthentication[R]
    with JsonEntitiesFromCodec[R]
    with BasicAuthTestApi
    with EndpointsTestApi
    with JsonFromPlayJsonCodecTestApi

class EndpointsTestSync
  extends EndpointsTestSuite[TestClient[Try]]
    with BasicAuthTestSuite[TestClient[Try]]
    with JsonFromCodecTestSuite[TestClient[Try]] {

  val backend = TryHttpURLConnectionBackend()

  val client: TestClient[Try] = new TestClient(s"http://localhost:$wiremockPort", backend)

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) = {
    Future.fromTry(endpoint(args))
  }

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
}

class EndpointsTestAkka
  extends EndpointsTestSuite[TestClient[Future]]
    with BasicAuthTestSuite[TestClient[Future]]
    with JsonFromCodecTestSuite[TestClient[Future]] {

  import ExecutionContext.Implicits.global

  val backend = AkkaHttpBackend()

  val client: TestClient[Future] = new TestClient(s"http://localhost:$wiremockPort", backend)

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req) = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
}

