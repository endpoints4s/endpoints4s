package endpoints.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import endpoints.testsuite.{BasicAuthTestApi, SimpleTestApi}
import endpoints.testsuite.client.{BasicAuthTestSuite, SimpleTestSuite}

import scala.concurrent.{ExecutionContext, Future}

class TestClient(settings: EndpointsSettings)
                (implicit EC: ExecutionContext, M: Materializer)
  extends Endpoints(settings)
  with BasicAuthTestApi
  with SimpleTestApi
  with BasicAuthentication

class EndpointsTest extends SimpleTestSuite[TestClient] with BasicAuthTestSuite[TestClient] {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val client: TestClient = new TestClient(EndpointsSettings(AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost", wiremockPort)))

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
}
