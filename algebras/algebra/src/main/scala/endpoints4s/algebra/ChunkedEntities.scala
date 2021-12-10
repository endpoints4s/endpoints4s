package endpoints4s.algebra

/** Algebra interface for describing request and response entities that use the chunked transfer-encoding.
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
trait ChunkedEntities
    extends EndpointsWithCustomErrors
    with ChunkedRequestEntities
    with ChunkedResponseEntities

trait Chunks {

  /** A stream of chunks of type `A`.
    *
    * @tparam A Information carried by each chunk
    * @group types
    */
  type Chunks[A]
}

trait ChunkedRequestEntities extends Chunks {
  this: EndpointsWithCustomErrors =>

  //TODO mention reframing
  /** A request entity carrying chunks of `String` values
    *
    * @group operations
    */
  def textChunksRequest: RequestEntity[Chunks[String]]

  //TODO mention reframing
  /** A request entity carrying chunks of `Array[Byte]` values
    *
    * @group operations
    */
  def bytesChunksRequest: RequestEntity[Chunks[Array[Byte]]]
}

trait ChunkedResponseEntities extends Chunks {
  this: EndpointsWithCustomErrors =>

  //TODO mention reframing
  /** A response entity carrying chunks of `String` values
    *
    * @group operations
    */
  def textChunksResponse: ResponseEntity[Chunks[String]]

  //TODO mention reframing
  /** A response entity carrying chunks of `Array[Byte]` values
    *
    * @group operations
    */
  def bytesChunksResponse: ResponseEntity[Chunks[Array[Byte]]]
}

/** Enriches the [[ChunkedEntities]] algebra with constructors of request
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
trait ChunkedJsonEntities extends ChunkedJsonRequestEntities with ChunkedJsonResponseEntities

trait ChunkedJsonRequestEntities
    extends ChunkedRequestEntities
    with JsonCodecs
    with RequestFraming {

  @deprecated(
    "Use jsonChunksRequest[A](framing: RequestFraming) instead to explicitly provide chunk framing",
    "1.6.1"
  )
  def jsonChunksRequest[A](implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]]

  /** A request entity carrying chunks of JSON values
    *
    * @tparam A Type of values serialized into JSON
    * @group operations
    */
  def jsonChunksRequest[A](framing: RequestFraming)(implicit
      codec: JsonCodec[A]
  ): RequestEntity[Chunks[A]]
}

trait ChunkedJsonResponseEntities
    extends ChunkedResponseEntities
    with JsonCodecs
    with ResponseFraming {

  @deprecated(
    "Use jsonChunksResponse[A](framing: ResponseFraming) instead to explicitly provide chunk framing",
    "1.6.1"
  )
  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]]

  /** A response entity carrying chunks of JSON values
    *
    * @tparam A Type of values serialized into JSON
    * @group operations
    */
  def jsonChunksResponse[A](framing: ResponseFraming)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[Chunks[A]]
}

trait RequestFraming {

  type RequestFraming
  def newLineDelimiterRequestFraming[A]: RequestFraming
}

trait ResponseFraming {

  type ResponseFraming
  def newLineDelimiterResponseFraming[A]: ResponseFraming
}
