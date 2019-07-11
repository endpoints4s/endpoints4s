package endpoints.algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing endpoints whose responses are streamed,
  * and WebSocket endpoints.
  *
  * The `chunkedEndpoint` method constructs an endpoint whose response is streamed
  * using the “Chunked Transfer-Encoding”.
  *
  * The `webSocketEndpoint` method constructs a WebSocket endpoint.
  *
  * Both kinds of endpoints manipulate intermittent streams of data, modeled by the
  * `Chunks[A]` type. The `bytesChunks` method constructs a stream of `Array[Byte]`,
  * whereas the `textChunks` method constructs a stream of `String`.
  *
  * @group algebras
  */
trait Http1Streaming extends Endpoints {

  /**
    * A stream of chunks containing values of type `A`.
    *
    * Values carried by the chunks are serialized to a binary or text format
    * whose type is unknown here: it does not show up as a parameter. We model
    * that “Serialized” type as an abstract type member in operations that
    * provide values of type `Chunks` so that clients and servers are guaranteed
    * to agree on that serialized type. See [[Http1JsonStreaming.jsonChunks]] as an
    * example.
    *
    * @tparam A Information carried by the chunk
    */
  type Chunks[A]

  /**
    * An endpoint with a request carrying an information of type `A` and a
    * response ''streaming'' chunks of type `B`.
    *
    * `ChunkedEndpoint[A, B]` should be equivalent to `Endpoint[A, Chunks[B]]`,
    * but a new type is used for convenience: indeed, most clients interpreters
    * define `type Endpoint[A, B] = A => Future[B]`, which would give
    * `type ChunkedEndpoint[A, B] = A => Future[Chunks[B]]`, which is often less
    * convenient to work with than just `A => Chunks[B]`.
    */
  type ChunkedEndpoint[A, B]

  /**
    * @return An endpoint that uses the chunked transfer encoding to stream
    *         its response elements of type `B`
    * @tparam A Information carried by the URL
    * @tparam B Information carried by each response chunk
    * @param summary optional summary documentation
    * @param description optional description documentation
    * @param tags list of OpenApi tags
    */
  def chunkedEndpoint[A, B](
    request: Request[A],
    responseChunks: Chunks[B],
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): ChunkedEndpoint[A, B]

  /**
    * A WebSocket endpoint whose URL carries an information of type `A`.
    * The WebSocket receives messages of type `B` and sends messages of type `C`.
    */
  type WebSocketEndpoint[A, B, C]

  /**
    * @return A WebSocket endpoint that receives messages of type `B` and sends
    *         messages of type `C`
    * @tparam A Information carried by the URL
    * @tparam B Information carried by each request message
    * @tparam C Information carried by each response message
    * @tparam S Type of serialized messages. Ideally, it would not show up in
    *           the signature and would be some existential type that
    *           `requestChunks` and `responseChunks` agree on.
    * @param summary optional summary documentation
    * @param description optional description documentation
    * @param tags list of OpenApi tags
    */
  def webSocketEndpoint[A, B, C, S](
    url: Url[A],
    requestChunks: Chunks[B] { type Serialized = S },
    responseChunks: Chunks[C] { type Serialized = S },
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): WebSocketEndpoint[A, B, C]

  /**
    * @return Chunks containing arrays of bytes
    */
  def bytesChunks(docs: Documentation = None): Chunks[Array[Byte]] { type Serialized = Array[Byte] }

  /**
    * @return Chunks containing text
    */
  def textChunks(docs: Documentation = None): Chunks[String] { type Serialized = String }

}

/**
  * Algebra interface for describing chunks that are serialized in JSON.
  *
  * @group algebras
  */
trait Http1JsonStreaming extends Http1Streaming with JsonCodecs {

  /** Chunks containing values of type `A` serialized into JSON.
    * We model the JSON type with type `String` here (see the `Serialized` type member)
    * because that’s what our `JsonCodec` works with. This allows us to support any
    * concrete JSON library (e.g. circe, play-json, etc.).
    */
  def jsonChunks[A](docs: Documentation = None)(implicit codec: JsonCodec[A]): Chunks[A] { type Serialized = String }

}
