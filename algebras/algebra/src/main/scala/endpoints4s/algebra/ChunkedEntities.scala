package endpoints4s.algebra

/** Algebra interface for describing request and response entities that use the chunked transfer-encoding.
  *
  * It introduces a type `Chunks[A]`, which models a stream of chunks of type `A`.
  * It also introduces constructors for chunked request and response entities.
  *
  * Chunk re-framing can happen during transport and is dependant on the specific implementation. It is known that
  * browser clients re-frame chunks received from the server.
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

/** @group algebras */
trait Chunks {

  /** A stream of chunks of type `A`.
    *
    * @tparam A Information carried by each chunk
    * @group types
    */
  type Chunks[A]
}

/** @group algebras */
trait ChunkedRequestEntities extends Chunks {
  this: EndpointsWithCustomErrors =>

  /** A request entity carrying chunks of `String` values
    *
    * @group operations
    */
  def textChunksRequest: RequestEntity[this.Chunks[String]]

  /** A request entity carrying chunks of `Array[Byte]` values
    *
    * @group operations
    */
  def bytesChunksRequest: RequestEntity[this.Chunks[Array[Byte]]]
}

/** @group algebras */
trait ChunkedResponseEntities extends Chunks {
  this: EndpointsWithCustomErrors =>

  /** A response entity carrying chunks of `String` values
    *
    * @group operations
    */
  def textChunksResponse: ResponseEntity[this.Chunks[String]]

  /** A response entity carrying chunks of `Array[Byte]` values
    *
    * @group operations
    */
  def bytesChunksResponse: ResponseEntity[this.Chunks[Array[Byte]]]
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
trait ChunkedJsonEntities
    extends ChunkedEntities
    with ChunkedJsonRequestEntities
    with ChunkedJsonResponseEntities

/** @group algebras */
trait ChunkedJsonRequestEntities extends ChunkedRequestEntities with JsonCodecs with Framing {

  @deprecated(
    "Use jsonChunksRequest[A](framing: RequestFraming) instead to explicitly provide chunk framing",
    "1.7.0"
  )
  def jsonChunksRequest[A](implicit
      codec: JsonCodec[A]
  ): RequestEntity[this.Chunks[A]]

  /** A request entity carrying chunks of JSON values
    *
    * @param framing Framing applied to chunks
    * @tparam A Type of values serialized into JSON
    * @group operations
    */
  def jsonChunksRequest[A](framing: this.Framing)(implicit
      codec: JsonCodec[A]
  ): RequestEntity[this.Chunks[A]] = unsupportedInterpreter(algebraVersion = "1.7.0")
}

/** @group algebras */
trait ChunkedJsonResponseEntities extends ChunkedResponseEntities with JsonCodecs with Framing {

  @deprecated(
    "Use jsonChunksResponse[A](framing: ResponseFraming) instead to explicitly provide chunk framing",
    "1.7.0"
  )
  def jsonChunksResponse[A](implicit
      codec: JsonCodec[A]
  ): ResponseEntity[this.Chunks[A]]

  /** A response entity carrying chunks of JSON values
    *
    * @param framing Framing applied to chunks
    * @tparam A Type of values serialized into JSON
    * @group operations
    */
  def jsonChunksResponse[A](framing: this.Framing)(implicit
      codec: JsonCodec[A]
  ): ResponseEntity[this.Chunks[A]] = unsupportedInterpreter(algebraVersion = "1.7.0")
}

/** Algebra interface for describing how chunks of chunked transfer-encoding requests and responses should be framed.
  * Being explicit about how chunks are framed solves the issue of re-framing happening during transport.
  * @group algebras
  */
trait Framing {

  /** A strategy for delimiting chunk frames in a stream of chunks. Framing has to be consistent between client and server -
    * server interpreter must be able to decode chunks encoded by the client, and vice versa.
    * @group types
    */
  type Framing

  /** Frames are delimited by a new-line separator
    * @group operations
    */
  def newLineDelimiterFraming: this.Framing = unsupportedInterpreter(algebraVersion = "1.7.0")
}
