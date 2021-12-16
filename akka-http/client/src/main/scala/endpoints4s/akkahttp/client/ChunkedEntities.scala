package endpoints4s.akkahttp.client

import akka.NotUsed
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity}
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Framing
import akka.stream.scaladsl.Source
import akka.util.ByteString
import endpoints4s.algebra

import scala.concurrent.Future

/** Interpreter for the [[algebra.ChunkedEntities]] algebra in the [[endpoints4s.akkahttp.client]] family.
  *
  * @group interpreters
  */
trait ChunkedEntities extends algebra.ChunkedEntities with EndpointsWithCustomErrors {

  //#stream-type
  type Chunks[A] = akka.stream.scaladsl.Source[A, _]
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

/** Interpreter for the [[algebra.ChunkedJsonEntities]] algebra in the [[endpoints4s.akkahttp.client]] family.
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

  def jsonChunksRequest[A](framing: Framing)(implicit
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

  def jsonChunksResponse[A](framing: Framing)(implicit
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

  trait Framing {
    def request: Flow[ByteString, ByteString, NotUsed]
    def response: Flow[ByteString, ByteString, NotUsed]
  }

  def newLineDelimiterFraming: Framing = new Framing {
    def request = Flow[ByteString].intersperse(ByteString("\n"))

    def response = Flow[ByteString].via(
      Framing.delimiter(
        ByteString("\n"),
        maximumFrameLength = Int.MaxValue,
        allowTruncation = true
      )
    )
  }

  private def noopFraming: Framing = new Framing {
    def request: Flow[ByteString, ByteString, NotUsed] = Flow.apply[ByteString]
    def response: Flow[ByteString, ByteString, NotUsed] = Flow.apply[ByteString]
  }
}
