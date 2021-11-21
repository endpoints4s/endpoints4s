package endpoints4s.algebra

trait ChunkedJsonEntitiesTestApi
    extends EndpointsTestApi
    with ChunkedJsonEntities
    with JsonEntitiesFromCodecs {

  case class Counter(value: Int)

  implicit def counterCodec: JsonCodec[Counter]

  val streamedEndpointTest: Endpoint[Unit, Chunks[Counter]] =
    endpoint(get(path / "notifications"), ok(jsonChunksResponse[Counter]))

  val streamedEndpointErrorTest: Endpoint[Unit, Chunks[Counter]] =
    endpoint(get(path / "notifications/error"), ok(jsonChunksResponse[Counter]))

  val streamedTextEndpointTest: Endpoint[Unit, Chunks[String]] =
    endpoint(get(path / "text"), ok(textChunksResponse))

  val streamedBytesEndpointTest: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "bytes"), ok(bytesChunksResponse))

  val uploadEndpointTest: Endpoint[Chunks[Array[Byte]], String] =
    endpoint(post(path / "upload", bytesChunksRequest), ok(textResponse))

  val streamedJsonUpload: Endpoint[Chunks[Counter], String] =
    endpoint(
      post(path / "counter-values", jsonChunksRequest[Counter]),
      ok(textResponse)
    )

}
