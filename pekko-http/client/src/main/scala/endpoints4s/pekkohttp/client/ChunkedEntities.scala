package endpoints4s.pekkohttp.client

import org.apache.pekko.NotUsed
import org.apache.pekko.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity}
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.{Framing => PekkoFraming}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import endpoints4s.algebra

import scala.concurrent.Future

/** Interpreter for the [[algebra.ChunkedEntities]] algebra in the [[endpoints4s.pekkohttp.client]] family.
  *
  * @group interpreters
  */
trait ChunkedEntities extends algebra.ChunkedEntities with EndpointsWithCustomErrors {

  //#stream-type
  type Chunks[A] = org.apache.pekko.stream.scaladsl.Source[A, _]
  //#stream-type

  def textChunksRequest: RequestEntity[Chunks[String]] =
    chunkedRequestEntity(
      ContentTypes.`text/plain(UTF-8)`,
      ByteString.fromString
    )

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    chunkedResponseEntity(byteString => Right(byteString.utf8String))

  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]] =
    chunkedRequestEntity(ContentTypes.`application/octet-stream`, ByteString(_))

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    chunkedResponseEntity(byteString => Right(byteString.toArray))

  private[client] def chunkedRequestEntity[A](
      contentType: ContentType,
      toByteString: A => ByteString,
      framing: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(identity)
  ): RequestEntity[Chunks[A]] =
    (as, httpRequest) =>
      httpRequest.withEntity(
        HttpEntity.Chunked.fromData(contentType, as.map(toByteString).via(framing))
      )

  private[client] def chunkedResponseEntity[A](
      fromByteString: ByteString => Either[Throwable, A],
      framing: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(identity)
  ): ResponseEntity[Chunks[A]] =
    httpEntity =>
      Future.successful(
        Right(httpEntity.dataBytes.via(framing).map(fromByteString).flatMapConcat {
          case Left(error)  => Source.failed(error)
          case Right(value) => Source.single(value)
        })
      )

}

/** Interpreter for the [[algebra.ChunkedJsonEntities]] algebra in the [[endpoints4s.pekkohttp.client]] family.
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
    val encoder = stringCodec(codec)
    chunkedRequestEntity(
      ContentTypes.`application/json`,
      a => ByteString.fromString(encoder.encode(a)),
      framing.request
    )
  }

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = jsonChunksResponse(noopFraming)

  override def jsonChunksResponse[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = {
    val decoder = stringCodec(codec)
    chunkedResponseEntity(
      { byteString =>
        val string = byteString.utf8String
        decoder
          .decode(string)
          .toEither
          .left
          .map(errors => new Throwable(errors.mkString(". ")))
      },
      framing.response
    )
  }

  class Framing(
      val request: Flow[ByteString, ByteString, NotUsed],
      val response: Flow[ByteString, ByteString, NotUsed]
  )

  override lazy val newLineDelimiterFraming: Framing = new Framing(
    Flow[ByteString].intersperse(ByteString("\n")),
    Flow[ByteString].via(
      PekkoFraming.delimiter(
        ByteString("\n"),
        maximumFrameLength = Int.MaxValue,
        allowTruncation = true
      )
    )
  )

  private lazy val noopFraming: Framing =
    new Framing(Flow.apply[ByteString], Flow.apply[ByteString])
}
