package endpoints.akkahttp.server

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import endpoints.algebra
import endpoints.algebra.{Codec, Documentation}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * @group interpreters
  */
trait Http1Streaming extends algebra.Http1Streaming with Endpoints {

  trait Chunks[A] {
    type Serialized
    def codec: Codec[Serialized, A]
    def chunkToByteString(chunk: Serialized): ByteString
    def contentType: ContentType
    def toMessageFlow(flow: Flow[Serialized, Serialized, _]): Flow[Message, Message, _]
  }

  class ChunkedEndpoint[A, B](request: Request[A], responseChunks: Chunks[B]) {
    def implementedBy(f: A => Source[B, _]): Route =
      request { a =>
        val responseEntity =
          HttpEntity.Chunked.fromData(
            responseChunks.contentType,
            f(a).map(b => responseChunks.chunkToByteString(responseChunks.codec.encode(b)))
          )
        Directives.complete((StatusCodes.OK, responseEntity))
      }
  }

  def chunkedEndpoint[A, B](
    request: Request[A],
    responseChunks: Chunks[B],
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): ChunkedEndpoint[A, B] =
    new ChunkedEndpoint(request, responseChunks)

  trait WebSocketEndpoint[A, B, C] { self =>
    type Serialized
    def url: Url[A]
    def requestChunks: Chunks[B] { type Serialized = self.Serialized }
    def responseChunks: Chunks[C] { type Serialized = self.Serialized }

    def implementedBy(f: A => Future[Option[Flow[B, C, _]]]): Route =
      url.directive { a =>
        Directives.onComplete(f(a)) {
          case Failure(_) => Directives.complete(HttpResponse(StatusCodes.InternalServerError))
          case Success(None) => malformedRequest
          case Success(Some(flow)) =>
            val requestFlow: Flow[Serialized, B, _] =
              Flow[Serialized]
                .mapAsync(1)(chunk => Future.fromTry(requestChunks.codec.decodeToTry(chunk)))
            val serverFlow =
              requestFlow.via(flow.map(responseChunks.codec.encode))
            Directives.handleWebSocketMessages(requestChunks.toMessageFlow(serverFlow))
        }
      }
  }

  def webSocketEndpoint[A, B, C, S](
    url: Url[A],
    requestChunks: Chunks[B] { type Serialized = S },
    responseChunks: Chunks[C] { type Serialized = S },
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): WebSocketEndpoint[A, B, C] = {
    def urlParam = url
    def requestChunksParam = requestChunks
    def responseChunksParam = responseChunks
    new WebSocketEndpoint[A, B, C] {
      type Serialized = S
      def url = urlParam
      def requestChunks = requestChunksParam
      def responseChunks = responseChunksParam
    }
  }

  def settings: Http1Streaming.Settings

  def bytesChunks(docs: Documentation): Chunks[Array[Byte]] { type Serialized = Array[Byte] } =
    new Chunks[Array[Byte]] {
      type Serialized = Array[Byte]
      def codec: Codec[Array[Byte], Array[Byte]] =
        new Codec[Array[Byte], Array[Byte]] {
          def decode(from: Array[Byte]): Either[Exception, Array[Byte]] = Right(from)
          def encode(from: Array[Byte]): Array[Byte] = from
        }
      def chunkToByteString(chunk: Array[Byte]): ByteString = ByteString(chunk)
      def contentType: ContentType = ContentTypes.`application/octet-stream`
      def toMessageFlow(flow: Flow[Array[Byte], Array[Byte], _]): Flow[Message, Message, _] = {
        val inFlow = Flow[Message].mapAsync(1) {
          case bm: BinaryMessage =>
            bm.toStrict(settings.toStrictTimeout)(settings.materializer)
              .map(_.data.toArray)(settings.executionContext)
          case _ => Future.failed(new Exception("Unexpected text message"))
        }
        val outFlow = flow.map(bytes => BinaryMessage(ByteString(bytes)))
        inFlow.via(outFlow)
      }
    }

  def textChunks(docs: Documentation): Chunks[String] { type Serialized = String } =
    new Chunks[String] {
      type Serialized = String
      def codec: Codec[String, String] =
        new Codec[String, String] {
          def decode(from: String): Either[Exception, String] = Right(from)
          def encode(from: String): String = from
        }
      def chunkToByteString(chunk: String): ByteString = ByteString.fromString(chunk)
      def contentType: ContentType = ContentTypes.`text/plain(UTF-8)`
      def toMessageFlow(flow: Flow[String, String, _]): Flow[Message, Message, _] = {
        val inFlow = Flow[Message].mapAsync(1) {
          case tm: TextMessage =>
            tm.toStrict(settings.toStrictTimeout)(settings.materializer)
              .map(_.text)(settings.executionContext)
          case _ => Future.failed(new Exception("Unexpected binary message"))
        }
        val outFlow = flow.map(TextMessage(_))
        inFlow.via(outFlow)
      }
    }

}

/**
  * @group interpreters
  */
trait Http1JsonStreaming
  extends algebra.Http1JsonStreaming
    with algebra.JsonEntitiesFromCodec
    with Http1Streaming {

  def jsonChunks[A](docs: Documentation)(implicit jsonCodec: JsonCodec[A]): Chunks[A] { type Serialized = String } =
    new Chunks[A] {
      type Serialized = String
      def codec: Codec[String, A] = jsonCodecToCodec(jsonCodec)
      def chunkToByteString(chunk: Serialized): ByteString = ByteString.fromString(chunk)
      def contentType: ContentType = ContentTypes.`application/json`
      def toMessageFlow(flow: Flow[String, String, _]): Flow[Message, Message, _] = {
        val inFlow = Flow[Message].mapAsync(1) {
          case tm: TextMessage =>
            tm.toStrict(settings.toStrictTimeout)(settings.materializer)
              .map(_.text)(settings.executionContext)
          case _ => Future.failed(new Exception("Unexpected binary message"))
        }
        val outFlow = flow.map(TextMessage(_))
        inFlow.via(outFlow)
      }
    }

}

object Http1Streaming {

  class Settings(
    val executionContext: ExecutionContext,
    val materializer: Materializer,
    val toStrictTimeout: FiniteDuration
  )

}
