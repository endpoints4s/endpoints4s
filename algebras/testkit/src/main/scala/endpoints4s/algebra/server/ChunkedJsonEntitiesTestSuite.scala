package endpoints4s.algebra.server

import org.apache.pekko.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  StatusCodes
}
import org.apache.pekko.stream.scaladsl.Framing
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.pattern.{after => pekkoAfter}
import org.apache.pekko.util.ByteString
import endpoints4s.Codec
import endpoints4s.algebra.ChunkedEntitiesTestApi
import endpoints4s.algebra.ChunkedJsonEntitiesTestApi

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

trait ChunkedJsonEntitiesTestSuite[
    T <: ChunkedEntitiesTestApi with ChunkedJsonEntitiesTestApi
] extends EndpointsTestSuite[T] {

  def serveStreamedEndpoint[Req, Resp, Mat](
      endpoint: serverApi.Endpoint[Req, serverApi.Chunks[Resp]],
      response: Source[Resp, Mat]
  )(
      runTests: Int => Unit
  ): Unit

  def serveStreamedEndpoint[Req, Resp](
      endpoint: serverApi.Endpoint[serverApi.Chunks[Req], Resp],
      logic: Source[Req, _] => Future[Resp]
  )(
      runTests: Int => Unit
  ): Unit

  def sendAndCollectResponseChunks(
      request: HttpRequest
  ): Future[(HttpResponse, Seq[ByteString])] = {
    httpClient.singleRequest(request).flatMap { response =>
      response.entity.dataBytes.runWith(Sink.seq).map { chunks =>
        (response, chunks)
      }
    }
  }

  def sendAndDecodeJsonChunks[A](request: HttpRequest)(implicit
      codec: serverApi.JsonCodec[A]
  ): Future[(HttpResponse, Seq[Either[Throwable, A]])] = {
    val jsonCodec: Codec[String, A] = serverApi.stringCodec(codec)
    httpClient.singleRequest(request).flatMap { response =>
      val chunksSource =
        response.entity.dataBytes
          .via(
            Framing.delimiter(
              ByteString("\n"),
              maximumFrameLength = Int.MaxValue,
              allowTruncation = true
            )
          )
          .map(chunk =>
            Right(
              jsonCodec
                .decode(decodeEntityAsText(response, chunk))
                .toEither
                .toOption
                .get
            )
          )
          .recover { case NonFatal(e) => Left(e) }
      chunksSource.runWith(Sink.seq).map { as => (response, as) }
    }
  }

  "Http1Streaming" should {

    "stream response entities" in {
      val expectedItems =
        serverApi.Counter(1) :: serverApi.Counter(2) :: serverApi.Counter(3) :: Nil
      serveStreamedEndpoint(
        serverApi.streamedEndpointTest,
        Source(expectedItems)
      ) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/notifications")
        whenReady(sendAndDecodeJsonChunks(request)(serverApi.counterCodec)) { case (_, chunks) =>
          assert(chunks.size == expectedItems.size)
          assert(
            chunks
              .zip(expectedItems)
              .forall { case (received, expected) =>
                received.toOption.contains(expected)
              }
          )
          ()
        }
      }
    }

    "close connection in case of error" in {
      val source: Source[serverApi.Counter, _] =
        Source((1 to 3).map(serverApi.Counter(_))).flatMapConcat { case serverApi.Counter(n) =>
          // adding a bit of delay to make sure connection does not fail before emitting chunks
          val delay = 1.second
          Source.futureSource(pekkoAfter(delay)(Future.successful {
            if (n == 3) {
              Source.failed(new Exception("Something went wrong"))
            } else {
              Source.single(serverApi.Counter(n))
            }
          }))
        }
      serveStreamedEndpoint(serverApi.streamedEndpointTest, source) { port =>
        val request = HttpRequest(uri = s"http://localhost:$port/notifications")
        whenReady(sendAndDecodeJsonChunks(request)(serverApi.counterCodec)) {
          case (response, chunks) =>
            assert(response.status == StatusCodes.OK)
            chunks.toList match {
              // Ideally, we would check that the first two elements were sent by the client
              // However, Pekko HTTP server implementation seems to always keep at least one chunk in the buffer,
              // meaning that a chunk is only emitted over TCP once a subsequent chunk is received by the
              // underlying buffer implementation.
              // Settings under pekko.stream.materializer.io.tcp are documented to allow disabling this behavior,
              // but they do not seem to actually allow that.
              // case Right(serverApi.Counter(1)) :: Right(serverApi.Counter(2)) :: Left(_) :: Nil => ()
              case chunks if chunks.exists(_.isLeft) => ()
              case chunks                            => fail(s"Unexpected chunks: $chunks")
            }
            ()
        }
      }
    }

    "stream request entities" in {
      val requestItems =
        Source(
          List(
            Array[Byte](1),
            Array[Byte](2),
            Array[Byte](3),
            Array[Byte](4),
            Array[Byte](5),
            Array[Byte](6)
          )
        )
      serveStreamedEndpoint[Array[Byte], String](
        serverApi.uploadEndpointTest,
        bytes => bytes.runWith(Sink.seq).map(_.flatten.mkString(";"))
      ) { port =>
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/upload",
          entity = HttpEntity.Chunked.fromData(
            ContentTypes.`application/octet-stream`,
            requestItems.map(ByteString(_))
          )
        )
        whenReady(send(request)) { case (_, byteString) =>
          assert(byteString.utf8String == "1;2;3;4;5;6")
          ()
        }
      }
    }

    "report decoding failure of incorrect elements in the stream" in {
      serveStreamedEndpoint[serverApi.Counter, String](
        serverApi.streamedJsonUpload,
        counters =>
          counters
            .map(Right(_))
            .recover { case NonFatal(exn) => Left(exn.toString) }
            .runWith(Sink.seq)
            .map(_.mkString(";"))
      ) { port =>
        val request = HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:$port/counter-values",
          entity = HttpEntity.Chunked.fromData(
            ContentTypes.`application/json`,
            Source(
              List(ByteString("{\"value\":1}"), ByteString("{\"value\":true}"))
            ).intersperse(ByteString("\n"))
          )
        )
        whenReady(send(request)) { case (response, byteString) =>
          assert(response.status == StatusCodes.OK)
          assert(
            byteString.utf8String == "Right(Counter(1));Left(java.lang.Throwable: DecodingFailure at .value: Int)"
          )
          ()
        }
      }
    }

  }
}
