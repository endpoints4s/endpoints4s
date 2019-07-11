package endpoints.play.server

import akka.stream.scaladsl.{Flow, Source}
import endpoints.algebra
import endpoints.algebra.{Codec, Documentation}
import play.api.http.{ContentTypes, Writeable}
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc.{Call, Handler, RequestHeader, Results, WebSocket}

import scala.concurrent.Future

/**
  * Interpreter for the [[algebra.Http1Streaming]] algebra interface, using Play framework
  *
  * @group interpreters
  */
trait Http1Streaming extends algebra.Http1Streaming with Endpoints {

  import playComponents.executionContext

  trait Chunks[A] {
    type Serialized
    def codec: endpoints.algebra.Codec[Serialized, A]
    def writeable: Writeable[Serialized]
    def messageFlowTransformer: MessageFlowTransformer[Serialized, Serialized]
  }

  class ChunkedEndpoint[A, B](request: Request[A], responseChunks: Chunks[B]) {

    def call(a: A): Call = request.encode(a)

    def implementedBy(f: A => Source[B, _]): ToPlayHandler =
      new ToPlayHandler {
        def playHandler(header: RequestHeader): Option[Handler] =
          request.decode(header).map { bodyParser =>
            playComponents.actionBuilder(bodyParser) { request =>
              Results.Ok.chunked(
                f(request.body)
                  .map(responseChunks.codec.encode)
              )(responseChunks.writeable)
            }
          }
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

  // This one cannot be a class because of the `Serialized` existential type
  // here modeled with an abstract type member.
  trait WebSocketEndpoint[A, B, C] { self =>
    type Serialized
    def url: Url[A]
    def requestChunks: Chunks[B] { type Serialized = self.Serialized }
    def responseChunks: Chunks[C] { type Serialized = self.Serialized }

    def call(host: String, port: Int, isSecure: Boolean, a: A): Call =
      Call("GET", s"ws${if (isSecure) "s" else ""}://$host:$port${url.encodeUrl(a)}")

    /**
      * @param f Async implementation function turning the `A` information extracted
      *          from the request into a `Flow[B, C, _]` representing the server-side
      *          of the conversation. A bad request can be signaled by returning `None`
      */
    def implementedBy(f: A => Future[Option[Flow[B, C, _]]]): ToPlayHandler =
      new ToPlayHandler {
        def playHandler(header: RequestHeader): Option[Handler] =
          url.decodeUrl(header).map {
            case Left(result) => playComponents.actionBuilder(result)
            case Right(a) =>
              val requestFlow: Flow[Serialized, B, _] =
                Flow[Serialized]
                  .mapAsync(1)(chunk => Future.fromTry(requestChunks.codec.decodeToTry(chunk)))
              WebSocket.acceptOrResult { _ =>
                f(a).map(_.map(flow => requestFlow.via(flow.map(responseChunks.codec.encode))).toRight(Results.BadRequest))
              }(requestChunks.messageFlowTransformer)
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
  ) = {
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

  def bytesChunks(docs: Documentation): Chunks[Array[Byte]] { type Serialized = Array[Byte] } =
    new Chunks[Array[Byte]] {
      type Serialized = Array[Byte]
      def codec: Codec[Array[Byte], Array[Byte]] =
        new Codec[Array[Byte], Array[Byte]] {
          def decode(from: Array[Byte]): Either[Exception, Array[Byte]] = Right(from)
          def encode(from: Array[Byte]): Array[Byte] = from
        }
      def writeable: Writeable[Array[Byte]] = Writeable.wByteArray
      def messageFlowTransformer: MessageFlowTransformer[Array[Byte], Array[Byte]] = MessageFlowTransformer.byteArrayMessageFlowTransformer
    }

  def textChunks(docs: Documentation): Chunks[String] { type Serialized = String } =
    new Chunks[String] {
      type Serialized = String
      def codec: Codec[String, String] =
        new Codec[String, String] {
          def decode(from: String): Either[Exception, String] = Right(from)
          def encode(from: String): String = from
        }
      def writeable: Writeable[String] = Writeable.wString
      def messageFlowTransformer: MessageFlowTransformer[String, String] = MessageFlowTransformer.stringMessageFlowTransformer
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
      def codec = jsonCodecToCodec(jsonCodec)
      def writeable: Writeable[String] =
        Writeable(Writeable.wString.transform, Some(ContentTypes.JSON))
      def messageFlowTransformer = MessageFlowTransformer.stringMessageFlowTransformer
    }

}
