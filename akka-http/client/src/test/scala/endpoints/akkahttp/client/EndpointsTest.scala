package endpoints.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import endpoints.algebra._
import endpoints.algebra.client._
import endpoints.algebra.circe

import scala.concurrent.{ExecutionContext, Future}

class TestClient(settings: EndpointsSettings)
  (implicit EC: ExecutionContext, M: Materializer)
  extends Endpoints(settings)
    with BasicAuthentication
    with EndpointsTestApi
    with BasicAuthTestApi
    with JsonFromCodecTestApi
    with circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodec
    with circe.JsonEntitiesFromCodec

class EndpointsTest
  extends EndpointsTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient]
    with JsonFromCodecTestSuite[TestClient]
{

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val client: TestClient = new TestClient(EndpointsSettings(AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost", wiremockPort)))

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
}
