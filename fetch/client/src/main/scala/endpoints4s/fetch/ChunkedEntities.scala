package endpoints4s.fetch

import endpoints4s.algebra
import org.scalajs.dom
import org.scalajs.dom.ReadableStreamReader

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.|

trait ChunkedResponseEntities
    extends algebra.ChunkedResponseEntities
    with EndpointsWithCustomErrors {

  type Chunks[A] = dom.ReadableStream[A]

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    chunkedResponseEntity(uint8array => Right(new TextDecoder("utf-8").decode(uint8array)))

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    chunkedResponseEntity(uint8array => Right(uint8array.toArray.map(_.toByte)))

  private[fetch] def chunkedResponseEntity[A](
      fromUint8Array: Uint8Array => Either[Throwable, A],
      framing: dom.ReadableStream[Uint8Array] => dom.ReadableStream[Uint8Array] = identity
  ): ResponseEntity[Chunks[A]] = { response =>
    val readableStream = ReadableStream[A](
      new ReadableStreamUnderlyingSource[A] {
        start = js.defined((controller: ReadableStreamController[A]) => {
          def read(reader: ReadableStreamReader[Uint8Array]): js.Promise[Unit] = {
            reader
              .read()
              .`then`((chunk: dom.Chunk[Uint8Array]) => {
                if (chunk.done) {
                  controller.close(): Unit | js.Thenable[Unit]
                } else {
                  fromUint8Array(chunk.value)
                    .fold(
                      error => js.Promise.reject(error),
                      value => {
                        controller.enqueue(value)
                        read(reader): Unit | js.Thenable[Unit]
                      }
                    ): Unit | js.Thenable[Unit]
                }
              })
          }
          read(framing(response.body).getReader()): js.UndefOr[js.Promise[Unit]]
        }): js.UndefOr[js.Function1[ReadableStreamController[A], js.UndefOr[js.Promise[Unit]]]]
      }
    )
    js.Promise.resolve[Either[Throwable, dom.ReadableStream[A]]](
      Right(readableStream.asInstanceOf[dom.ReadableStream[A]])
    )
  }
}

trait Framing extends algebra.Framing {
  class Framing(val response: dom.ReadableStream[Uint8Array] => dom.ReadableStream[Uint8Array])
}

trait ChunkedJsonResponseEntities
    extends algebra.ChunkedJsonResponseEntities
    with ChunkedResponseEntities
    with JsonEntitiesFromCodecs
    with Framing {

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = jsonChunksResponse(noopFraming)

  override def jsonChunksResponse[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = {
    val decoder = stringCodec(codec)
    chunkedResponseEntity(
      { uint8array =>
        val string = new TextDecoder("utf-8").decode(uint8array)
        decoder
          .decode(string)
          .toEither
          .left
          .map(errors => new Throwable(errors.mkString(". ")))
      },
      framing.response
    )
  }

  override lazy val newLineDelimiterFraming: Framing = new Framing(readableStream =>
    ReadableStream[Uint8Array](
      new ReadableStreamUnderlyingSource[Uint8Array] {
        start = js.defined((controller: ReadableStreamController[Uint8Array]) => {
          def read(
              reader: ReadableStreamReader[Uint8Array],
              buffer: String,
              firstChunk: Boolean
          ): js.Promise[Unit] = {
            reader
              .read()
              .`then`((chunk: dom.Chunk[Uint8Array]) => {
                if (chunk.done) {
                  if (!firstChunk) {
                    controller.enqueue(new TextEncoder("utf-8").encode(buffer))
                  }
                  controller.close(): Unit | js.Thenable[Unit]
                } else {
                  val newBuffer = (buffer + new TextDecoder("utf-8").decode(chunk.value))
                    .foldLeft("") { case (tmpBuffer, char) =>
                      if (char == '\n') {
                        controller.enqueue(new TextEncoder("utf-8").encode(tmpBuffer))
                        ""
                      } else {
                        tmpBuffer + char
                      }
                    }
                  read(reader, newBuffer, firstChunk = false): Unit | js.Thenable[Unit]
                }
              })
          }
          read(readableStream.getReader(), "", firstChunk = true): js.UndefOr[js.Promise[Unit]]
        }): js.UndefOr[
          js.Function1[ReadableStreamController[Uint8Array], js.UndefOr[js.Promise[Unit]]]
        ]
      }
    ).asInstanceOf[dom.ReadableStream[Uint8Array]]
  )

  private lazy val noopFraming: Framing = new Framing(identity)
}
