package endpoints4s.pekkohttp.server

import endpoints4s.algebra
import org.apache.pekko.NotUsed
import org.apache.pekko.http.scaladsl.marshalling.Marshaller
import org.apache.pekko.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, HttpRequest, MessageEntity}
import org.apache.pekko.http.scaladsl.server.Directives
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.{Framing => PekkoFraming}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import scala.concurrent.Future

/** Interpreter for the [[algebra.ChunkedEntities]] algebra in the [[endpoints4s.pekkohttp.server]] family.
  *
  * @group interpreters
  */
trait ChunkedEntities extends algebra.ChunkedEntities with EndpointsWithCustomErrors {

  type Chunks[A] = Source[A, _]

  def textChunksRequest: RequestEntity[Chunks[String]] =
    chunkedRequestEntity(byteString => Right(byteString.utf8String))

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    chunkedResponseEntity(
      ContentTypes.`text/plain(UTF-8)`,
      ByteString.fromString
    )

  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]] =
    chunkedRequestEntity(byteString => Right(byteString.toArray))

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    chunkedResponseEntity(
      ContentTypes.`application/octet-stream`,
      ByteString.fromArray
    )

  private[server] def chunkedRequestEntity[A](
      fromByteString: ByteString => Either[Throwable, A],
      framing: Flow[ByteString, ByteString, NotUsed] = Flow.apply[ByteString]
  ): RequestEntity[Chunks[A]] =
    Directives.entity(Unmarshaller[HttpRequest, Chunks[A]] { _ => request =>
      val source = request.entity.dataBytes.via(framing).map(fromByteString).flatMapConcat {
        case Left(error)  => Source.failed(error)
        case Right(value) => Source.single(value)
      }
      Future.successful(source)
    })

  private[server] def chunkedResponseEntity[A](
      contentType: ContentType,
      toByteString: A => ByteString,
      framing: Flow[ByteString, ByteString, NotUsed] = Flow.apply[ByteString]
  ): ResponseEntity[Chunks[A]] =
    Marshaller.withFixedContentType[Chunks[A], MessageEntity](contentType) { as =>
      val byteStrings = as.map(toByteString)
      HttpEntity.Chunked.fromData(contentType, byteStrings.via(framing))
    }

}

/** Interpreter for the [[algebra.ChunkedJsonEntities]] algebra in the [[endpoints4s.pekkohttp.server]] family.
  *
  * @group interpreters
  */
trait ChunkedJsonEntities
    extends algebra.ChunkedJsonEntities
    with ChunkedEntities
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
      ContentTypes.`application/json`,
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
      PekkoFraming.delimiter(
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
