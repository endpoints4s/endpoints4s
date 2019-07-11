package endpoints.algebra.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, PeerClosedConnectionException}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.{ActorMaterializer, Materializer}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, SinkQueueWithCancel, Source, SourceQueueWithComplete}
import endpoints.algebra.Http1JsonStreamingTestApi

import scala.concurrent.Future

trait JsonStreamingTestSuite[T <: Http1JsonStreamingTestApi] extends ClientTestBase[T] {

  val streamingClient: T
  val streamingPort: Int = findOpenPort

  private implicit val serverSystem: ActorSystem = ActorSystem("server-system")
  private implicit val serverMaterializer: Materializer = ActorMaterializer()

  def serving[A](route: Route)(thunk: => Unit): Unit = {
    whenReady(Http().bindAndHandle(route, "localhost", streamingPort)) { binding =>
      thunk
      whenReady(binding.unbind())(_ => ())
    }
  }

  import streamingClient.{ChunkedEndpoint, WebSocketEndpoint, Counter, chunkedEndpointTest, webSocketEndpointTest}

  /**
    * Calls the endpoint and accumulates the messages sent by the server.
    * (only endpoints streaming a finite number of items can be tested)
    */
  def callChunkedEndpoint[A, B](endpoint: ChunkedEndpoint[A, B], req: A): Future[Seq[B]]

  def callWebSocketEndpoint[A, B, C](
    endpoint: WebSocketEndpoint[A, B, C],
    req: A
  ): Future[Option[(SourceQueueWithComplete[B], SinkQueueWithCancel[C])]]

  "Decode chunks streamed by a server" in {

    val expectedItems = Counter(1) :: Counter(2) :: Counter(3) :: Nil

    serving(
      (get & path("notifications")) {
        complete(HttpEntity.Chunked(
          ContentTypes.`application/json`,
          Source(List("{\"value\":1}", "{\"value\":2}", "{\"value\":3}"))
        ))
      }
    ) {
      whenReady(callChunkedEndpoint(chunkedEndpointTest, ()))(_ shouldEqual expectedItems)
      ()
    }

  }

  "Handle WebSockets" in {

    val webSocketRoute =
      path("ping-pong") {
        // Messages are directly sent back to the client without modification
        handleWebSocketMessages(Flow[Message])
      }

    serving(webSocketRoute) {
      whenReady(callWebSocketEndpoint(webSocketEndpointTest, ())) {
        case None => fail("Unable to open WebSocket")
        case Some((sourceQueue, sinkQueue)) =>
          val elem = Counter(1)
          whenReady(sourceQueue.offer(elem)) { _ =>
            whenReady(sinkQueue.pull()) {
              case Some(`elem`) =>
                sourceQueue.complete()
                whenReady(sinkQueue.pull()) {
                  case None => () // The connexion was closed because the source was completed
                  case _ => fail("Unexpected message")
                }
              case _ => fail("Unable to decode message")
            }
          }
      }
      ()
    }

    serving(webSocketRoute) {
      whenReady(callWebSocketEndpoint(webSocketEndpointTest, ())) {
        case None => fail("Unable to open WebSocket")
        case Some((sourceQueue, sinkQueue)) =>
          sourceQueue.fail(new Exception("Unable to send message"))
          whenReady(sinkQueue.pull().failed) {
            case _: PeerClosedConnectionException => ()
            case _ => fail()
          }
      }
    }

  }

}
