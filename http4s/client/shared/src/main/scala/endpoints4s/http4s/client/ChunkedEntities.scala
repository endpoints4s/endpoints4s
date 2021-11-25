package endpoints4s.http4s.client

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
  ): RequestEntity[Chunks[A]] = { (stream, req) =>
    val encoder = stringCodec(codec)
    req.withEntity(stream.map(encoder.encode))
  }

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] = { response =>
    val decoder = stringCodec[A](codec)

    val stream = response.bodyText.evalMap(s =>
      decoder
        .decode(s)
        .fold(
          effect.pure,
          es => effect.raiseError[A](new Throwable(es.mkString(", ")))
        )
    )
    effect.pure(stream)

  }

}
