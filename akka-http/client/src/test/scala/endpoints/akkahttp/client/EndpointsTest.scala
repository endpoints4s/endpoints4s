package endpoints.akkahttp.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Flow, Sink, SinkQueueWithCancel, Source, SourceQueueWithComplete}
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy}
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
    with Http1JsonStreaming
    with Http1JsonStreamingTestApi
    with circe.Http1JsonStreamingTestApi

class EndpointsTest
  extends EndpointsTestSuite[TestClient]
    with BasicAuthTestSuite[TestClient]
    with JsonFromCodecTestSuite[TestClient]
    with JsonStreamingTestSuite[TestClient]
{

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val client: TestClient =
    new TestClient(
      EndpointsSettings(
        Http(),
        "localhost", wiremockPort,
        AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost", wiremockPort)
      )
    )

  val streamingClient: TestClient =
    new TestClient(
      EndpointsSettings(
        Http(),
        "localhost", streamingPort,
        AkkaHttpRequestExecutor.cachedHostConnectionPool("localhost", streamingPort)
      )
    )

  def call[Req, Resp](endpoint: client.Endpoint[Req, Resp], args: Req): Future[Resp] = endpoint(args)

  def callChunkedEndpoint[A, B](endpoint: streamingClient.ChunkedEndpoint[A, B], req: A): Future[Seq[B]] =
    endpoint(req).runWith(Sink.seq)

  def callWebSocketEndpoint[A, B, C](endpoint: streamingClient.WebSocketEndpoint[A, B, C], req: A): Future[Option[(SourceQueueWithComplete[B], SinkQueueWithCancel[C])]] = {
    val source = Source.queue[B](8, OverflowStrategy.fail)
    val sink = Sink.queue[C]()
    val flow = Flow.fromSinkAndSourceMat(sink, source)((l, r) => (r, l))
    val (eventualDone, mat) = endpoint(req, flow)
    eventualDone.map(maybeDone => maybeDone.map(_ => mat))
  }

  def encodeUrl[A](url: client.Url[A])(a: A): String = url.encode(a)

  clientTestSuite()
  basicAuthSuite()
  jsonFromCodecTestSuite()

}
