package endpoints.algebra.server

import akka.http.scaladsl.model.ws.{Message, PeerClosedConnectionException, TextMessage, WebSocketRequest}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Keep, Sink, SinkQueueWithCancel, Source, SourceQueueWithComplete}
import akka.util.ByteString
import endpoints.algebra.{Codec, Http1JsonStreamingTestApi}

import scala.concurrent.Future
import scala.util.control.NonFatal

trait Http1JsonStreamingTestSuite[T <: Http1JsonStreamingTestApi] extends EndpointsTestSuite[T] {

  /**
    * @param runTests A function that is called after the server is started and before it is stopped. It takes
    *                 the TCP port number as parameter.
    */
  def serveChunkedEndpoint[Resp](
    endpoint: serverApi.ChunkedEndpoint[_, Resp],
    response: Source[Resp, _]
  )(
    runTests: Int => Unit
  ): Unit

  def serveWebSocketEndpoint[Req, Resp](
    endpoint: serverApi.WebSocketEndpoint[_, Req, Resp],
    serverFlow: Flow[Req, Resp, _]
  )(
    runTests: Int => Unit
  ): Unit

  def sendAndCollectResponseChunks(request: HttpRequest): Future[(HttpResponse, Seq[ByteString])] = {
    httpClient.singleRequest(request).flatMap { response =>
      response.entity.dataBytes.runWith(Sink.seq).map { chunks =>
        (response, chunks)
      }
    }
  }

  def sendAndDecodeJsonChunks[A](request: HttpRequest)(implicit codec: serverApi.JsonCodec[A]): Future[(HttpResponse, Seq[Either[Throwable, A]])] = {
    val jsonCodec: Codec[String, A] = serverApi.jsonCodecToCodec(codec)
    httpClient.singleRequest(request).flatMap { response =>
      val chunksSource =
        response.entity.dataBytes
          .map(chunk => Right(jsonCodec.decodeToTry(decodeEntityAsText(response, chunk)).get))
          .recoverWithRetries(1, { case NonFatal(e) => Source.single(Left(e)) })
      chunksSource.runWith(Sink.seq).map { as =>
        (response, as)
      }
    }
  }

  def sendAndOpenWebSocket[A](
    request: WebSocketRequest,
    clientFlow: Flow[Message, Message, A]
  ): Future[A] = {
    val (eventualResponse, a) = httpClient.singleWebSocketRequest(request, clientFlow)
    eventualResponse.map(_ => a)
  }

  "Http1Streaming" should {

    "use chunked transfer-encoding" in {
      val expectedItems =
        serverApi.Counter(1) :: serverApi.Counter(2) :: serverApi.Counter(3) :: Nil
      serveChunkedEndpoint(serverApi.chunkedEndpointTest, Source(expectedItems)) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/notifications")
        whenReady(sendAndDecodeJsonChunks(request)(serverApi.counterCodec)) { case (response, chunks) =>
          assert(chunks.zip(expectedItems)
            .forall { case (received, expected) => received.right.toOption.contains(expected) })
          ()
        }
      }
    }

    "close connection in case of error" in {
      val source = Source.unfoldAsync(1) { n =>
        if (n < 3) Future.successful(Some((n + 1, serverApi.Counter(n))))
        else Future.failed(new Exception("Something went wrong"))
      }
      serveChunkedEndpoint(serverApi.chunkedEndpointTest, source) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/notifications")
        whenReady(sendAndDecodeJsonChunks(request)(serverApi.counterCodec)) { case (response, chunks) =>
          chunks.toList match {
            case Right(serverApi.Counter(1)) :: Right(serverApi.Counter(2)) :: Left(_) :: Nil => ()
            case chunks => fail(s"Unexpected chunks: $chunks")
          }
          ()
        }
      }
    }

    "handle WebSocket" in {
      val serverFlow =
        Flow.fromFunction((c: serverApi.Counter) => serverApi.Counter(c.value + 1))
      val clientSink = Sink.queue[serverApi.Counter]()
      val clientSource = Source.queue[serverApi.Counter](1, OverflowStrategy.fail)
      val clientFlow = Flow.fromSinkAndSourceMat(clientSink, clientSource)(Keep.both)
      val clientMessageFlow: Flow[Message, Message, (SinkQueueWithCancel[serverApi.Counter], SourceQueueWithComplete[serverApi.Counter])] =
        Flow[Message].mapAsync(1) {
          case tm: TextMessage =>
            tm.toStrict(patienceConfig.timeout).flatMap(m =>
              Future.fromTry(serverApi.jsonCodecToCodec(serverApi.counterCodec).decodeToTry(m.text))
            )
          case _ => Future.failed(new Exception("Unexpected WebSocket message type"))
        }.viaMat(
          clientFlow.map(b =>
            TextMessage(serverApi.jsonCodecToCodec(serverApi.counterCodec).encode(b))
          )
        )(Keep.right)
      serveWebSocketEndpoint(serverApi.webSocketEndpointTest, serverFlow) { port =>
        val request = WebSocketRequest(uri = s"ws://localhost:$port/ping-pong")
        whenReady(sendAndOpenWebSocket(request, clientMessageFlow)) { case (sinkQueue, sourceQueue) =>
          sourceQueue.offer(serverApi.Counter(1))
          whenReady(sinkQueue.pull()) { maybeCounter =>
            assert(maybeCounter.contains(serverApi.Counter(2)))
          }
          sourceQueue.offer(serverApi.Counter(10))
          whenReady(sinkQueue.pull()) { maybeCounter =>
            assert(maybeCounter.contains(serverApi.Counter(11)))
          }
          sourceQueue.complete()
          whenReady(sinkQueue.pull()) { maybeCounter =>
            assert(maybeCounter.isEmpty)
          }
          ()
        }
      }

      serveWebSocketEndpoint(serverApi.webSocketEndpointTest, serverFlow) { port =>
        val request = WebSocketRequest(uri = s"ws://localhost:$port/ping-pong")
        whenReady(sendAndOpenWebSocket(request, clientMessageFlow)) { case (sinkQueue, sourceQueue) =>
          sourceQueue.fail(new Exception("Unable to send message"))
          whenReady(sinkQueue.pull().failed) {
            case _: PeerClosedConnectionException => ()
            case _ => fail()
          }
          ()
        }
      }
    }
  }

}
