package endpoints.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import endpoints.algebra._
import endpoints.algebra.client._

import scala.concurrent.{ExecutionContext, Future}

//TODO should we test JsonFromCodecTestApi here or just in supported json backends?
class TestClient(settings: EndpointsSettings)
  (implicit EC: ExecutionContext, M: Materializer)
  extends Endpoints(settings)
    with BasicAuthentication
    with EndpointsTestApi
    with BasicAuthTestApi
//    with JsonEntitiesFromCodec
//    with JsonFromCodecTestApi

class EndpointsTest
  extends EndpointsTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient]
//    with JsonFromCodecTestSuite[TestClient]
{

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val client: TestClient = new TestClient(EndpointsSettings(AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost", wiremockPort)))

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  clientTestSuite()
  basicAuthSuite()
  //  jsonFromCodecTestSuite()
}
