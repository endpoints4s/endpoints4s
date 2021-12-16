package endpoints4s.http4s.client

import fs2.Chunk
import fs2.Pipe
import fs2.Pull
import fs2.Stream
import org.http4s.{Request => Http4sRequest, Response => Http4sResponse}

trait ChunkedEntities extends endpoints4s.algebra.ChunkedEntities with EndpointsWithCustomErrors {

  //#stream-type
  type Chunks[A] = fs2.Stream[Effect, A]
  //#stream-type

  override def textChunksRequest: (Chunks[String], Http4sRequest[Effect]) => Http4sRequest[Effect] =
    (stream, req) => req.withEntity(stream)

  override def textChunksResponse: Http4sResponse[Effect] => Effect[Chunks[String]] =
    res => effect.pure(res.bodyText)

  override def bytesChunksRequest
      : (Chunks[Array[Byte]], Http4sRequest[Effect]) => Http4sRequest[Effect] =
    (stream, req) => req.withEntity(stream)

  override def bytesChunksResponse: Http4sResponse[Effect] => Effect[Chunks[Array[Byte]]] =
    res => effect.pure(res.body.chunks.map(_.toArray))

}

trait ChunkedJsonEntities
    extends endpoints4s.algebra.ChunkedJsonEntities
    with ChunkedEntities
    with JsonEntitiesFromCodecs {

  def jsonChunksRequest[A](implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = jsonChunksRequest(noopFraming)

  def jsonChunksRequest[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] = { (stream, req) =>
    val encoder = stringCodec(codec)
    req.withEntity(stream.map(encoder.encode).through(framing.request))
  }

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = jsonChunksResponse(noopFraming)

  def jsonChunksResponse[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = { response =>
    val decoder = stringCodec[A](codec)

    val stream = response.bodyText
      .through(framing.response)
      .evalMap(s =>
        decoder
          .decode(s)
          .fold(
            effect.pure,
            es => effect.raiseError[A](new Throwable(es.mkString(", ")))
          )
      )
    effect.pure(stream)

  }

  trait Framing {
    def request: Pipe[Effect, String, String]
    def response: Pipe[Effect, String, String]
  }

  def newLineDelimiterFraming: Framing = new Framing {
    def request = in => in.intersperse("\n")

    def response = {
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
  }

  private def noopFraming: Framing = new Framing {
    def request: Pipe[Effect, String, String] = identity
    def response: Pipe[Effect, String, String] = identity
  }
}
