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

  type RequestFraming = Flow[ByteString, ByteString, NotUsed]

  type ResponseFraming = Flow[ByteString, ByteString, NotUsed]

  def jsonChunksRequest[A](implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = jsonChunksRequest(Flow.apply[ByteString])

  def jsonChunksRequest[A](framing: RequestFraming)(implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = {
    val encoder = stringCodec(codec)
    chunkedRequestEntity(
      ContentTypes.`application/json`,
      a => ByteString.fromString(encoder.encode(a)),
      framing
    )
  }

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = jsonChunksResponse(Flow.apply[ByteString])

  def jsonChunksResponse[A](framing: ResponseFraming)(implicit
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
      framing
    )
  }

  def newLineDelimiterRequestFraming[A]: RequestFraming =
    Flow[ByteString].intersperse(ByteString("\n"))

  def newLineDelimiterResponseFraming[A]: ResponseFraming =
    Flow[ByteString].via(
      Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue, allowTruncation = true)
    )
}
