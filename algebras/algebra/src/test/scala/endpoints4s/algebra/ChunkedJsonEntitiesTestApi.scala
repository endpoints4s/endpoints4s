package endpoints4s.algebra

trait ChunkedJsonEntitiesTestApi
    extends EndpointsTestApi
    with ChunkedJsonEntities
    with JsonEntitiesFromCodecs {

  case class Counter(value: Int)

  implicit def counterCodec: JsonCodec[Counter]

  val streamedEndpointTest: Endpoint[Unit, Chunks[Counter]] =
    endpoint(get(path / "notifications"), ok(jsonChunksResponse[Counter]))

  val uploadEndpointTest: Endpoint[Chunks[Array[Byte]], String] =
    endpoint(post(path / "upload", bytesChunksRequest), ok(textResponse))

  val streamedJsonUpload: Endpoint[Chunks[Counter], String] =
    endpoint(
      post(path / "counter-values", jsonChunksRequest[Counter]),
      ok(textResponse)
    )

}
