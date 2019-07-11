package endpoints.akkahttp.client

import java.nio.charset.StandardCharsets.UTF_8

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.util.ByteString
import endpoints.algebra
import endpoints.algebra.{Codec, Documentation}

import scala.concurrent.Future

/**
  * @group interpreters
  */
trait Http1Streaming extends algebra.Http1Streaming { this: Endpoints =>

  abstract class Chunks[A] {
    type Serialized
    def decodeByteString: Flow[ByteString, A, _]
    def encodeWebSocketMessage(a: A): Message
    def decodeWebSocketMessage(message: Message): Future[A]
  }

  //#chunked-endpoint-type
  type ChunkedEndpoint[A, B] = A => Source[B, _]
  //#chunked-endpoint-type

  def chunkedEndpoint[A, B](
    request: Request[A],
    responseChunks: Chunks[B],
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): ChunkedEndpoint[A, B] =
    a =>
      Source.fromFutureSource(
        request(a).map { response =>
          response.entity.dataBytes.via(responseChunks.decodeByteString)
        }
      )

  //#websocket-endpoint-type
  trait WebSocketEndpoint[A, B, C] {
    /**
      * @param a Information carried by the URL
      * @param clientFlow A flow representing the client-side of the WebSocket
      *                   conversation: it sends messages of type `B` and receives
      *                   messages of type `C`
      * @return A `Future`, completed with `Some(Done)` when the WebSocket connection is
      *         established or `None` if the request was rejected, and
      *         the `clientFlow` materialized value
      */
    def apply[Mat](a: A, clientFlow: Flow[C, B, Mat]): (Future[Option[Done]], Mat)
  }
  //#websocket-endpoint-type

  def webSocketEndpoint[A, B, C, S](
    url: Url[A],
    requestChunks: Chunks[B] { type Serialized = S },
    responseChunks: Chunks[C] { type Serialized = S },
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): WebSocketEndpoint[A, B, C] = new WebSocketEndpoint[A, B, C] {
    def apply[Mat](a: A, clientFlow: Flow[C, B, Mat]): (Future[Option[Done]], Mat) = {
      val messageToMessage: Flow[Message, Message, Mat] =
        Flow[Message]
          .mapAsync(1)(responseChunks.decodeWebSocketMessage)
          .viaMat(clientFlow.map(requestChunks.encodeWebSocketMessage))(Keep.right)
      // TODO Safer URL construction. wss support.
      val wsUrl = s"ws://${settings.host}:${settings.port}${url.encode(a)}"
      val (eventualUpgrade, mat) =
        settings.httpExt.singleWebSocketRequest(WebSocketRequest(wsUrl), messageToMessage)
      val eventualDone =
        eventualUpgrade.map { upgrade =>
          if (upgrade.response.status == StatusCodes.SwitchingProtocols) Some(Done)
          else if (upgrade.response.status == StatusCodes.BadRequest) None
          else sys.error(s"Unable to open WebSocket: ${upgrade.response.status}")
        }
      (eventualDone, mat)
    }
  }

  def bytesChunks(docs: Documentation): Chunks[Array[Byte]] { type Serialized = Array[Byte] } =
    new Chunks[Array[Byte]] {
      type Serialized = Array[Byte]
      def decodeByteString: Flow[ByteString, Array[Byte], _] =
        Flow.fromFunction((chunk: ByteString) => chunk.toArray)
      def encodeWebSocketMessage(bytes: Array[Byte]): Message = BinaryMessage(ByteString(bytes))
      def decodeWebSocketMessage(message: Message): Future[Array[Byte]] =
        message match {
          case bm: BinaryMessage => bm.toStrict(settings.toStrictTimeout).map(_.data.toArray)
          case _ => Future.failed(new Exception("Unexpected text message"))
        }
    }

  def textChunks(docs: Documentation): Chunks[String] { type Serialized = String } =
    new Chunks[String] {
      type Serialized = String
      def decodeByteString: Flow[ByteString, String, _] =
        Flow.fromFunction((chunk: ByteString) => chunk.decodeString(UTF_8))
      def encodeWebSocketMessage(s: String): Message = TextMessage(s)
      def decodeWebSocketMessage(message: Message): Future[String] =
        message match {
          case tm: TextMessage => tm.toStrict(settings.toStrictTimeout).map(_.text)
          case _ => Future.failed(new Exception("Unexpected binary message"))
        }
    }

}

/**
  * @group interpreters
  */
trait Http1JsonStreaming extends algebra.Http1JsonStreaming with algebra.JsonEntitiesFromCodec with Http1Streaming { this: Endpoints =>

  def jsonChunks[A](docs: Documentation)(implicit schema: JsonCodec[A]): Chunks[A] { type Serialized = String } =
    new Chunks[A] {
      type Serialized = String
      val codec: Codec[String, A] = jsonCodecToCodec(schema)
      def decodeByteString: Flow[ByteString, A, _] =
        Flow[ByteString]
          .mapAsync(1)(chunk => Future.fromTry(codec.decodeToTry(chunk.decodeString(UTF_8))))
      def encodeWebSocketMessage(a: A): Message = TextMessage(codec.encode(a))
      def decodeWebSocketMessage(message: Message): Future[A] =
        message match {
          case tm: TextMessage =>
            tm.toStrict(settings.toStrictTimeout)
              .flatMap(m => Future.fromTry(codec.decodeToTry(m.text)))
          case _ => Future.failed(new Exception("Unexpected binary message"))
        }
    }

}
