package endpoints4s.openapi

import endpoints4s.algebra
import endpoints4s.openapi.model.{MediaType, Schema}

/** Interpreter for the [[algebra.ChunkedEntities]] algebra in the [[endpoints4s.openapi]] family.
  *
  * @group interpreters
  */
trait ChunkedEntities extends algebra.ChunkedEntities with EndpointsWithCustomErrors {

  type Chunks[A] = Unit

  private lazy val textChunksEntity =
    Map("text/plain" -> MediaType(Some(Schema.simpleString)))

  def textChunksRequest: RequestEntity[Chunks[String]] = textChunksEntity

  def textChunksResponse: ResponseEntity[Chunks[String]] = textChunksEntity

  private lazy val bytesChunksEntity =
    Map("application/octet-stream" -> MediaType(None))

  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]] = bytesChunksEntity

  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]] =
    bytesChunksEntity

}

/** Interpreter for the [[algebra.ChunkedJsonEntities]] algebra in the [[endpoints4s.openapi]] family.
  *
  * @group interpreters
  */
trait ChunkedJsonEntities
    extends algebra.ChunkedJsonEntities
    with ChunkedEntities
    with JsonEntitiesFromSchemas {

  private def jsonChunksEntity[A](codec: JsonCodec[A]) =
    Map("application/json" -> MediaType(Some(toSchema(codec.docs))))

  type Framing = Unit

  def jsonChunksRequest[A](implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] =
    jsonChunksEntity(codec)

  override def jsonChunksRequest[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]] =
    jsonChunksEntity(codec)

  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] =
    jsonChunksEntity(codec)

  override def jsonChunksResponse[A](framing: Framing)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]] =
    jsonChunksEntity(codec)

  override def newLineDelimiterFraming: Framing = ()
}
