package endpoints.algebra

/**
  * Algebra interface for describing request and response entities that use the chunked transfer-encoding.
  *
  * It introduces a type `Chunks[A]`, which models a stream of chunks of type `A`.
  * It also introduces constructors for chunked request and response entities.
  *
  * Example:
  *
  * {{{
  *   val notifications: Endpoint[Unit, Chunks[String]] =
  *     endpoint(
  *       get(path / "notifications"),
  *       ok(textChunksResponse)
  *     )
  * }}}
  *
  * Or also:
  *
  * {{{
  *   val upload: Endpoint[Chunks[Array[Byte]], Unit] =
  *     endpoint(
  *       post(path / "upload", bytesChunksRequest),
  *       ok(emptyResponse)
  *     )
  * }}}
  *
  * @group algebras
  */
trait ChunkedEntities extends EndpointsWithCustomErrors {

  /**
    * A stream of chunks of type `A`.
    *
    * @tparam A Information carried by each chunk
    * @group types
    */
  type Chunks[A]

  /**
    * A request entity carrying chunks of `String` values
    *
    * @group operations
    */
  def textChunksRequest: RequestEntity[Chunks[String]]

  /**
    * A response entity carrying chunks of `String` values
    *
    * @group operations
    */
  def textChunksResponse: ResponseEntity[Chunks[String]]

  /**
    * A request entity carrying chunks of `Array[Byte]` values
    *
    * @group operations
    */
  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]]

  /**
    * A response entity carrying chunks of `Array[Byte]` values
    *
    * @group operations
    */
  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]]

}

/**
  * Enriches the [[ChunkedEntities]] algebra with constructors of request
  * and response entities carrying JSON documents.
  *
  * Example:
  *
  * {{{
  *   val events =
  *     endpoint(
  *       get(path / "events"),
  *       ok(jsonChunksResponse[Event])
  *     )
  * }}}
  *
  * @group algebras
  */
trait ChunkedJsonEntities extends ChunkedEntities with JsonCodecs {

  /**
    * A request entity carrying chunks of JSON values
    *
    * @tparam A Type of values serialized into JSON
    * @group operations
    */
  def jsonChunksRequest[A](implicit codec: JsonCodec[A]): RequestEntity[Chunks[A]]

  /**
    * A response entity carrying chunks of JSON values
    *
    * @tparam A Type of values serialized into JSON
    * @group operations
    */
  def jsonChunksResponse[A](implicit codec: JsonCodec[A]): ResponseEntity[Chunks[A]]

}
