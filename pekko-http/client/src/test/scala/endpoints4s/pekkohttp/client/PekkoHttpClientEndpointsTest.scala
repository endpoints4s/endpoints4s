package endpoints4s.pekkohttp.client

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.testkit.TestKit
import endpoints4s.algebra
import endpoints4s.algebra.ChunkedEntitiesTestApi
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
    with ChunkedEntitiesTestApi
    with ChunkedJsonEntitiesTestApi
    with algebra.circe.CounterCodecCirce

class PekkoHttpClientEndpointsTest
    extends algebra.client.EndpointsTestSuite[TestClient]
    with algebra.client.BasicAuthTestSuite[TestClient]
    with algebra.client.JsonFromCodecTestSuite[TestClient]
    with algebra.client.TextEntitiesTestSuite[TestClient]
    with algebra.client.SumTypedEntitiesTestSuite[TestClient]
    with algebra.client.ChunkedEntitiesTestSuite[TestClient]
    with algebra.client.ChunkedJsonEntitiesTestSuite[TestClient] {

  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  val client: TestClient = new TestClient(
    EndpointsSettings(
      PekkoHttpRequestExecutor
        .cachedHostConnectionPool("localhost", stubServerPortHTTP)
    )
  )

  val streamingClient: TestClient = new TestClient(
    EndpointsSettings(
      PekkoHttpRequestExecutor
        .cachedHostConnectionPool("localhost", stubServerPortHTTP)
    )
  )

  def call[Req, Resp](
      endpoint: client.Endpoint[Req, Resp],
      args: Req
  ): Future[Resp] = endpoint(args)

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
      req: Seq[A]
  ): Future[B] =
    endpoint(Source.fromIterator(() => req.iterator))

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
