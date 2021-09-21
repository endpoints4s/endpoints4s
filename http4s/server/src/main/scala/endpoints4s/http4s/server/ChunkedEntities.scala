package endpoints4s.http4s.server

import endpoints4s.algebra
import org.http4s.EntityEncoder
import org.http4s.MediaType
import org.http4s.Entity
import org.http4s.headers.`Content-Type`
import fs2.Chunk

trait ChunkedEntities extends algebra.ChunkedEntities with EndpointsWithCustomErrors {

  type Chunks[A] = fs2.Stream[Effect, A]

  def textChunksRequest: RequestEntity[Chunks[String]] =
    req => Effect.pure(Right(req.bodyText))

  def textChunksResponse: ResponseEntity[Chunks[String]] =
    EntityEncoder.encodeBy()(stream => Entity(stream.through(fs2.text.utf8Encode)))

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
  ): RequestEntity[Chunks[A]] = {
    val decoder = stringCodec(codec)
    req =>
      Effect.pure(
        Right(
          req.bodyText.evalMap(s =>
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
  ): ResponseEntity[Chunks[A]] = {
    val encoder = stringCodec(codec)
    EntityEncoder.encodeBy(`Content-Type`(MediaType.application.`json`))(stream =>
      Entity(stream.map(encoder.encode).through(fs2.text.utf8Encode))
    )
  }

}
