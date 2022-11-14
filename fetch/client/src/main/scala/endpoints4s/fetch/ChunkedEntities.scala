package endpoints4s.fetch

import endpoints4s.algebra
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.PropertyDescriptor
import scala.scalajs.js.typedarray.Uint8Array
import scala.scalajs.js.|

trait ChunkedEntities extends ChunkedRequestEntities with ChunkedResponseEntities

trait ChunkedRequestEntities
    extends algebra.ChunkedRequestEntities
    with EndpointsWithCustomErrors
    with Chunks {

  def chunksRequestDuplex: ChunksRequestDuplex

  def textChunksRequest: RequestEntity[Chunks[String]] =
    chunkedRequestEntity(string => new TextEncoder("utf-8").encode(string))

  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]] =
    chunkedRequestEntity(byteArray => Uint8Array.from(byteArray.map(_.toShort).toJSArray))

  private[fetch] def chunkedRequestEntity[A](
      toUint8Array: A => Uint8Array,
      framing: dom.ReadableStream[Uint8Array] => dom.ReadableStream[Uint8Array] = identity
  ): RequestEntity[Chunks[A]] = { (as, request) =>
    val readableStream = dom.ReadableStream[Uint8Array](
      new dom.ReadableStreamUnderlyingSource[Uint8Array] {
        start = js.defined((controller: dom.ReadableStreamController[Uint8Array]) => {
          def read(reader: dom.ReadableStreamReader[A]): js.Promise[Unit] = {
            reader
              .read()
              .`then`((chunk: dom.Chunk[A]) => {
                if (chunk.done) {
                  controller.close(): Unit | js.Thenable[Unit]
                } else {
                  controller.enqueue(toUint8Array(chunk.value))
                  read(reader): Unit | js.Thenable[Unit]
                }
              })
          }

          read(as.getReader()): js.UndefOr[js.Promise[Unit]]
        }): js.UndefOr[
          js.Function1[dom.ReadableStreamController[Uint8Array], js.UndefOr[js.Promise[Unit]]]
        ]
      }
    )
    request.body = framing(readableStream)
    // https://developer.chrome.com/articles/fetch-streaming-requests/#half-duplex
    js.Object.defineProperty(
      request,
      "duplex",
      new PropertyDescriptor {
        configurable = true
        enumerable = true
        value = chunksRequestDuplex
        writable = true
      }
    )
    ()
  }
}

trait ChunkedResponseEntities
    extends algebra.ChunkedResponseEntities
    with EndpointsWithCustomErrors
    with Chunks {

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    chunkedResponseEntity(uint8array => Right(new TextDecoder("utf-8").decode(uint8array)))

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    chunkedResponseEntity(uint8array => Right(uint8array.toArray.map(_.toByte)))

  private[fetch] def chunkedResponseEntity[A](
      fromUint8Array: Uint8Array => Either[Throwable, A],
      framing: dom.ReadableStream[Uint8Array] => dom.ReadableStream[Uint8Array] = identity
  ): ResponseEntity[Chunks[A]] = { response =>
    val readableStream = dom.ReadableStream[A](
      new dom.ReadableStreamUnderlyingSource[A] {
        start = js.defined((controller: dom.ReadableStreamController[A]) => {
          def read(reader: dom.ReadableStreamReader[Uint8Array]): js.Promise[Unit] = {
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
        }): js.UndefOr[js.Function1[dom.ReadableStreamController[A], js.UndefOr[js.Promise[Unit]]]]
      }
    )
    js.Promise.resolve[Either[Throwable, dom.ReadableStream[A]]](
      Right(readableStream)
    )
  }
}

trait Chunks {
  type Chunks[A] = dom.ReadableStream[A]
}

trait Framing extends algebra.Framing {
  class Framing(
      val request: dom.ReadableStream[Uint8Array] => dom.ReadableStream[Uint8Array],
      val response: dom.ReadableStream[Uint8Array] => dom.ReadableStream[Uint8Array]
  )

  override lazy val newLineDelimiterFraming: Framing = new Framing(
    readableStream =>
      dom.ReadableStream[Uint8Array](
        new dom.ReadableStreamUnderlyingSource[Uint8Array] {
          start = js.defined((controller: dom.ReadableStreamController[Uint8Array]) => {
            def read(
                reader: dom.ReadableStreamReader[Uint8Array]
            ): js.Promise[Unit] = {
              reader
                .read()
                .`then`((chunk: dom.Chunk[Uint8Array]) => {
                  if (chunk.done) {
                    controller.close(): Unit | js.Thenable[Unit]
                  } else {
                    controller.enqueue(chunk.value)
                    controller.enqueue(new TextEncoder("utf-8").encode("\n"))
                    read(reader): Unit | js.Thenable[Unit]
                  }
                })
            }

            read(readableStream.getReader()): js.UndefOr[
              js.Promise[Unit]
            ]
          }): js.UndefOr[
            js.Function1[dom.ReadableStreamController[Uint8Array], js.UndefOr[js.Promise[Unit]]]
          ]
        }
      ),
    readableStream =>
      dom.ReadableStream[Uint8Array](
        new dom.ReadableStreamUnderlyingSource[Uint8Array] {
          start = js.defined((controller: dom.ReadableStreamController[Uint8Array]) => {
            def read(
                reader: dom.ReadableStreamReader[Uint8Array],
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

            read(readableStream.getReader(), "", firstChunk = true): js.UndefOr[
              js.Promise[Unit]
            ]
          }): js.UndefOr[
            js.Function1[dom.ReadableStreamController[Uint8Array], js.UndefOr[js.Promise[Unit]]]
          ]
        }
      )
  )

  protected lazy val noopFraming: Framing = new Framing(identity, identity)
}

trait ChunkedJsonEntities extends ChunkedJsonRequestEntities with ChunkedJsonResponseEntities

trait ChunkedJsonRequestEntities
    extends algebra.ChunkedJsonRequestEntities
    with ChunkedRequestEntities
    with JsonEntitiesFromCodecs
    with Framing {
  def jsonChunksRequest[A](implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = jsonChunksRequest(noopFraming)

  override def jsonChunksRequest[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = {
    val encoder = stringCodec(codec)
    chunkedRequestEntity(
      { a =>
        val string = encoder.encode(a)
        new TextEncoder("utf-8").encode(string)
      },
      framing.request
    )
  }
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
}
