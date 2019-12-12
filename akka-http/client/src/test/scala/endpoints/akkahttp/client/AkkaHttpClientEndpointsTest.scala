package endpoints.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import endpoints.algebra

import scala.concurrent.{ExecutionContext, Future}

class TestClient(settings: EndpointsSettings)
  (implicit EC: ExecutionContext, M: Materializer)
  extends Endpoints(settings)
    with BasicAuthentication
    with algebra.EndpointsTestApi
    with algebra.BasicAuthTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodecs
    with algebra.circe.JsonEntitiesFromCodecs

class AkkaHttpClientEndpointsTest
  extends algebra.client.EndpointsTestSuite[TestClient]
    with algebra.client.BasicAuthTestSuite[TestClient]
    with algebra.client.JsonFromCodecTestSuite[TestClient]
{

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val client: TestClient = new TestClient(EndpointsSettings(AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost", wiremockPort)))

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
