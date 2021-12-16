package endpoints4s.http4s.server

import endpoints4s.algebra
import org.http4s.EntityEncoder
import org.http4s.MediaType
import org.http4s.Entity
import org.http4s.headers.`Content-Type`
import fs2.Chunk
import fs2.Pipe
import fs2.Pull
import fs2.Stream

trait ChunkedEntities extends algebra.ChunkedEntities with EndpointsWithCustomErrors {

  type Chunks[A] = fs2.Stream[Effect, A]

  def textChunksRequest: RequestEntity[Chunks[String]] =
    req => Effect.pure(Right(req.bodyText))

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    EntityEncoder.encodeBy()(stream => Entity(stream.through(fs2.text.utf8.encode)))

  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]] =
    req => Effect.pure(Right(req.body.chunks.map(_.toArray)))

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    EntityEncoder.encodeBy(
      `Content-Type`(MediaType.application.`octet-stream`)
    )(stream => Entity(stream.flatMap(arr => fs2.Stream.chunk(Chunk.array(arr)))))

}

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
    val decoder = stringCodec(codec)
    req =>
      Effect.pure(
        Right(
          req.bodyText
            .through(framing.request)
            .evalMap(s =>
              decoder
                .decode(s)
                .fold(
                  Effect.pure,
                  es => Effect.raiseError[A](new Throwable(es.mkString(", ")))
                )
            )
        )
      )
  }

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = jsonChunksResponse(noopFraming)

  def jsonChunksResponse[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = {
    val encoder = stringCodec(codec)
    EntityEncoder.encodeBy(`Content-Type`(MediaType.application.`json`))(stream =>
      Entity(
        stream.map(encoder.encode).through(framing.response).through(fs2.text.utf8.encode)
      )
    )
  }

  trait Framing {
    def request: Pipe[Effect, String, String]
    def response: Pipe[Effect, String, String]
  }

  def newLineDelimiterFraming: Framing = new Framing {
    def request = {
      def go(stream: Stream[Effect, String], buffer: StringBuilder): Pull[Effect, String, Unit] = {
        stream.pull.uncons.flatMap {
          case Some((head, tail)) =>
            val (pull, newBuffer) = buffer
              .append(head.iterator.mkString)
              .foldLeft((Pull.output(Chunk.empty[String]), new StringBuilder)) {
                case ((pullAcc, tmpBuffer), char) =>
                  if (char == '\n') {
                    (pullAcc >> Pull.output(Chunk(tmpBuffer.toString())), new StringBuilder)
                  } else {
                    (pullAcc, tmpBuffer.append(char))
                  }
              }
            pull >> go(tail, newBuffer)
          case None => Pull.output(Chunk(buffer.toString()))
        }
      }
      in => go(in, new StringBuilder).stream
    }

    def response = in => in.intersperse("\n")
  }

  private def noopFraming: Framing = new Framing {
    def request: Pipe[Effect, String, String] = identity
    def response: Pipe[Effect, String, String] = identity
  }
}
