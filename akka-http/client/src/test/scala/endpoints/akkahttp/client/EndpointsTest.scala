package endpoints.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import endpoints.testsuite.{BasicAuthTestApi, OptionalResponsesTestApi, SimpleTestApi}
import endpoints.testsuite.client.{BasicAuthTestSuite, OptionalResponsesTestSuite, SimpleTestSuite}

import scala.concurrent.{ExecutionContext, Future}

class TestClient(settings: EndpointsSettings)
                (implicit EC: ExecutionContext, M: Materializer)
  extends Endpoints(settings)
  with OptionalResponses
  with BasicAuthTestApi
  with SimpleTestApi
  with OptionalResponsesTestApi
  with BasicAuthentication

class EndpointsTest
  extends SimpleTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient]
    with OptionalResponsesTestSuite[TestClient] {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val client: TestClient = new TestClient(EndpointsSettings(AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost", wiremockPort)))

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  optionalResponsesSuite()
}
