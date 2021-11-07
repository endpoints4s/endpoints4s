package endpoints4s.akkahttp.client

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import endpoints4s.algebra
import endpoints4s.algebra.ChunkedJsonEntitiesTestApi

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class TestClient(settings: EndpointsSettings)(implicit
    EC: ExecutionContext,
    M: Materializer
) extends Endpoints(settings)
    with BasicAuthentication
    with algebra.client.ClientEndpointsTestApi
    with algebra.BasicAuthenticationTestApi
    with algebra.TextEntitiesTestApi
    with algebra.JsonFromCodecTestApi
    with algebra.SumTypedEntitiesTestApi
    with algebra.circe.JsonFromCirceCodecTestApi
    with JsonEntitiesFromCodecs
    with algebra.circe.JsonEntitiesFromCodecs
    with ChunkedJsonEntities
    with ChunkedJsonEntitiesTestApi
    with algebra.circe.ChunkedJsonEntitiesTestApi

class AkkaHttpClientEndpointsTest
    extends algebra.client.EndpointsTestSuite[TestClient]
    with algebra.client.BasicAuthTestSuite[TestClient]
    with algebra.client.JsonFromCodecTestSuite[TestClient]
    with algebra.client.TextEntitiesTestSuite[TestClient]
    with algebra.client.SumTypedEntitiesTestSuite[TestClient]
    with algebra.client.ChunkedJsonEntitiesTestSuite[TestClient] {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher

  val client: TestClient = new TestClient(
    EndpointsSettings(
      AkkaHttpRequestExecutor
        .cachedHostConnectionPool("localhost", wiremockPort)
    )
  )

  val streamingClient: TestClient = new TestClient(
    EndpointsSettings(
      AkkaHttpRequestExecutor
        .cachedHostConnectionPool("localhost", streamingPort)
    )
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[A, streamingClient.Chunks[B]],
      req: A
  ): Future[Seq[Either[String, B]]] =
    Source
      .futureSource(endpoint(req))
      .map(Right(_))
      .recover { case NonFatal(t) => Left(t.toString) }
      .runWith(Sink.seq)

  def callStreamedEndpoint[A, B](
      endpoint: streamingClient.Endpoint[streamingClient.Chunks[A], B],
      req: Source[A, _]
  ): Future[B] =
    endpoint(req)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()
  textEntitiesTestSuite()
  sumTypedRequestsTestSuite()

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
