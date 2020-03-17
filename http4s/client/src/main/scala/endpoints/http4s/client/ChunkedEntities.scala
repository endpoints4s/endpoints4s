package endpoints.http4s.client

import org.http4s.{Request => Http4sRequest, Response => Http4sResponse}
import fs2.Chunk

trait ChunkedEntities
    extends endpoints.algebra.ChunkedEntities
    with EndpointsWithCustomErrors {

  type Chunks[A] = fs2.Stream[Effect, A]

  override def textChunksRequest
      : (Chunks[String], Http4sRequest[Effect]) => Http4sRequest[Effect] =
    (stream, req) => req.withEntity(stream)

  override def textChunksResponse
      : Http4sResponse[Effect] => Effect[Chunks[String]] =
    res => effect.pure(res.bodyAsText)

  override def bytesChunksRequest
      : (Chunks[Array[Byte]], Http4sRequest[Effect]) => Http4sRequest[Effect] =
    (stream, req) =>
      req.withBodyStream(
        stream.flatMap(arr => fs2.Stream.chunk(Chunk.array(arr)))
      )

  override def bytesChunksResponse
      : Http4sResponse[Effect] => Effect[Chunks[Array[Byte]]] =
    res => effect.pure(res.body.chunks.map(_.toArray))

}
