package endpoints4s.fetch

import endpoints4s.algebra
import org.scalajs.dom.experimental.ByteString
import org.scalajs.dom.experimental.ReadableStream

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.typedarray.Uint8Array

trait ChunkedEntities extends algebra.ChunkedEntities with EndpointsWithCustomErrors {

  type Chunks[A] = ChunksSource[A]

  class ChunksSource[A](
      val fromByteString: ByteString => Either[Throwable, A],
      val readableStream: ReadableStream[Uint8Array]
  ) {

    //TODO since regular endpoints are split for Futures and Thenables, it could be done here too
    def apply(
        handler: A => Unit
    ): Future[Unit] = {
      val textDecoder = new TextDecoder("utf-8")
      val donePromise = Promise[Unit]()
      def read(): Future[Unit] = {
        readableStream
          .getReader()
          .read()
          .flatMap { chunk =>
            if (chunk.done) {
              donePromise.completeWith(Future.successful(()))
              Future.successful(())
            } else {
              fromByteString(textDecoder.decode(chunk.value))
                .fold(
                  error => {
                    donePromise.completeWith(Future.failed(error))
                    Future.successful(())
                  },
                  value => {
                    handler(value)
                    read()
                  }
                )
            }
          }
      }
      read()
      donePromise.future
    }
  }

  private[fetch] def chunkedResponseEntity[A](
      fromByteString: ByteString => Either[Throwable, A]
  ): ResponseEntity[Chunks[A]] =
    response => Future.successful(Right(new ChunksSource(fromByteString, response.body)))

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    chunkedResponseEntity(byteString => Right(byteString))

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    chunkedResponseEntity(byteString => Right(byteString.getBytes))

  //TODO complicated, needs some thought on how to write chunks to a body with native, well supported APIs
  def textChunksRequest: RequestEntity[Chunks[String]] = ???
  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]] = ???
}

trait ChunkedJsonEntities
    extends algebra.ChunkedJsonEntities
    with ChunkedEntities
    with JsonEntitiesFromCodecs {

  def jsonChunksRequest[A](implicit codec: JsonCodec[A]): RequestEntity[Chunks[A]] = ???

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = {
    val decoder = stringCodec(codec)
    chunkedResponseEntity { byteString =>
      val string = byteString
      decoder
        .decode(string)
        .toEither
        .left
        .map(errors => new Throwable(errors.mkString(". ")))
    }
  }
}
