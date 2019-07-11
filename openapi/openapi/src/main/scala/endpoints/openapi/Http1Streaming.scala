package endpoints.openapi

import endpoints.algebra
import endpoints.algebra.Documentation
import endpoints.openapi.model.{MediaType, Schema}

/**
  * @group interpreters
  */
trait Http1Streaming extends algebra.Http1Streaming with Endpoints {

  trait Chunks[A] {
    type Serialized
    val content: Map[String, MediaType]
    val docs: Documentation
  }
  def Chunks[A, S](_content: Map[String, MediaType], _docs: Documentation): Chunks[A] { type Serialized = S } =
    new Chunks[A] {
      type Serialized = S
      val content = _content
      val docs = _docs
    }

  type ChunkedEndpoint[A, B] = DocumentedEndpoint

  def chunkedEndpoint[A, B](
    request: DocumentedRequest,
    responseChunks: Chunks[B],
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): ChunkedEndpoint[A, B] =
    endpoint(
      request,
      DocumentedResponse(200, responseChunks.docs.getOrElse(""), responseChunks.content) :: Nil,
      summary,
      description,
      tags
    )

  type WebSocketEndpoint[A, B, C] = DocumentedEndpoint

  def webSocketEndpoint[A, B, C, S](
    url: DocumentedUrl,
    requestChunks: Chunks[B] { type Serialized = S },
    responseChunks: Chunks[C] { type Serialized = S },
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): WebSocketEndpoint[A, B, C] =
    endpoint(
      DocumentedRequest(Get, url, emptyHeaders, None),
      DocumentedResponse(101, "" /* temporary */, Map.empty /* temporary? */) :: Nil,
      summary,
      description,
      tags
    )

  def bytesChunks(docs: Documentation): Chunks[Array[Byte]] { type Serialized = Array[Byte] } =
    Chunks(Map("application/octet-stream" -> MediaType(None)), docs)

  def textChunks(docs: Documentation): Chunks[String] { type Serialized = String } =
    Chunks(Map("text/plain" -> MediaType(Some(Schema.simpleString))), docs)

}

/**
  * @group interpreters
  */
trait Http1JsonStreaming extends algebra.Http1JsonStreaming with Http1Streaming with JsonSchemaEntities {

  def jsonChunks[A](docs: Documentation)(implicit codec: DocumentedJsonSchema): Chunks[A] { type Serialized = String } =
    Chunks(Map("application/json" -> MediaType(Some(toSchema(codec)))), docs)

}
