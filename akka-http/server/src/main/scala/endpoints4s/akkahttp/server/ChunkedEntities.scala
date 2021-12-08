package endpoints4s.akkahttp.server

import akka.NotUsed
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, HttpRequest, MessageEntity}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Framing
import akka.stream.scaladsl.Source
import akka.util.ByteString
import endpoints4s.algebra
import endpoints4s.algebra.NewLineChunkCodec

import scala.concurrent.Future

/** Interpreter for the [[algebra.ChunkedEntities]] algebra in the [[endpoints4s.akkahttp.server]] family.
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
      chunkCodec: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(identity)
  ): RequestEntity[Chunks[A]] =
    Directives.entity(Unmarshaller[HttpRequest, Chunks[A]] { _ => request =>
      val source = request.entity.dataBytes.via(chunkCodec).map(fromByteString).flatMapConcat {
        case Left(error)  => Source.failed(error)
        case Right(value) => Source.single(value)
      }
      Future.successful(source)
    })

  private[server] def chunkedResponseEntity[A](
      contentType: ContentType,
      toByteString: A => ByteString,
      chunkCodec: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].map(identity)
  ): ResponseEntity[Chunks[A]] =
    Marshaller.withFixedContentType[Chunks[A], MessageEntity](contentType) { as =>
      val byteStrings = as.map(toByteString)
      HttpEntity.Chunked.fromData(contentType, byteStrings.via(chunkCodec))
    }

}

/** Interpreter for the [[algebra.ChunkedJsonEntities]] algebra in the [[endpoints4s.akkahttp.server]] family.
  *
  * @group interpreters
  */
trait ChunkedJsonEntities
    extends algebra.ChunkedJsonEntities
    with ChunkedEntities
    with JsonEntitiesFromCodecs
    with NewLineChunkCodec {

  type RequestChunkCodec = Flow[ByteString, ByteString, NotUsed]

  type ResponseChunkCodec = Flow[ByteString, ByteString, NotUsed]

  def jsonChunksRequest[A](chunkCodec: RequestChunkCodec)(implicit
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
      chunkCodec
    )
  }

  def jsonChunksResponse[A](chunkCodec: RequestChunkCodec)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = {
    val encoder = stringCodec(codec)
    chunkedResponseEntity(
      ContentTypes.`application/json`,
      a => ByteString(encoder.encode(a)),
      chunkCodec
    )
  }

  def newLineRequestChunkCodec[A]: RequestChunkCodec =
    Flow[ByteString].via(
      Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue, allowTruncation = true)
    )

  def newLineResponseChunkCodec[A]: ResponseChunkCodec =
    Flow[ByteString].intersperse(ByteString("\n"))
}
