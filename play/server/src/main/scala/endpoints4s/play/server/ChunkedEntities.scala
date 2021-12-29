package endpoints4s.play.server

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.{Framing => AkkaFraming}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import endpoints4s.algebra
import play.api.http.{ContentTypes, HttpChunk, HttpEntity}
import play.api.libs.streams.Accumulator
import play.api.mvc.BodyParser

/** Interpreter for the [[algebra.ChunkedEntities]] algebra in the [[endpoints4s.play.server]] family.
  *
  * @group interpreters
  */
trait ChunkedEntities extends EndpointsWithCustomErrors with algebra.ChunkedEntities {

  import playComponents.executionContext

  type Chunks[A] = Source[A, _]

  def textChunksRequest: RequestEntity[Chunks[String]] =
    chunkedRequestEntity(byteString => Right(byteString.utf8String))

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    chunkedResponseEntity(ContentTypes.TEXT, ByteString.fromString)

  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]] =
    chunkedRequestEntity(byteString => Right(byteString.toArray))

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    chunkedResponseEntity(ContentTypes.BINARY, ByteString.fromArray)

  private[server] def chunkedRequestEntity[A](
      fromByteString: ByteString => Either[Throwable, A],
      framing: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(identity)
  ): RequestEntity[Chunks[A]] =
    _ => {
      Some(BodyParser.apply { _ =>
        Accumulator.source[ByteString].map { byteStrings =>
          val source: Source[A, _] =
            byteStrings.via(framing).map(fromByteString).flatMapConcat {
              case Left(error)  => Source.failed(error)
              case Right(value) => Source.single(value)
            }
          Right(source)
        }
      })
    }

  private[server] def chunkedResponseEntity[A](
      contentType: String,
      toByteString: A => ByteString,
      framing: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(identity)
  ): ResponseEntity[Chunks[A]] =
    as => {
      val byteStrings = as.map(toByteString).via(framing).map(a => HttpChunk.Chunk(a))
      HttpEntity.Chunked(byteStrings, Some(contentType))
    }

}

/** Interpreter for the [[algebra.ChunkedJsonEntities]] algebra in the [[endpoints4s.play.server]] family.
  *
  * @group interpreters
  */
trait ChunkedJsonEntities
    extends ChunkedEntities
    with algebra.ChunkedJsonEntities
    with JsonEntitiesFromCodecs {

  def jsonChunksRequest[A](implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = jsonChunksRequest(noopFraming)

  override def jsonChunksRequest[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = {
    val decoder = stringCodec(codec)
    chunkedRequestEntity(
      { byteString =>
        val string = byteString.utf8String
        decoder
          .decode(string)
          .toEither
          .left
          .map(errors => new Throwable(errors.mkString(". ")))
      },
      framing.request
    )
  }

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = jsonChunksResponse(noopFraming)

  override def jsonChunksResponse[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = {
    val encoder = stringCodec(codec)
    chunkedResponseEntity(
      ContentTypes.JSON,
      a => ByteString(encoder.encode(a)),
      framing.response
    )
  }

  class Framing(
      val request: Flow[ByteString, ByteString, NotUsed],
      val response: Flow[ByteString, ByteString, NotUsed]
  )

  override lazy val newLineDelimiterFraming: Framing = new Framing(
    Flow[ByteString].via(
      AkkaFraming.delimiter(
        ByteString("\n"),
        maximumFrameLength = Int.MaxValue,
        allowTruncation = true
      )
    ),
    Flow[ByteString].intersperse(ByteString("\n"))
  )

  private lazy val noopFraming: Framing =
    new Framing(Flow.apply[ByteString], Flow.apply[ByteString])
}
