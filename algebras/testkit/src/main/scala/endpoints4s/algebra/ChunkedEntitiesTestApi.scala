package endpoints4s.algebra

trait ChunkedEntitiesTestApi
    extends ChunkedRequestEntitiesTestApi
    with ChunkedResponseEntitiesTestApi

trait ChunkedRequestEntitiesTestApi extends EndpointsTestApi with ChunkedRequestEntities {

  val uploadEndpointTest: Endpoint[Chunks[Array[Byte]], String] =
    endpoint(post(path / "upload", bytesChunksRequest), ok(textResponse))
}

trait ChunkedResponseEntitiesTestApi extends EndpointsTestApi with ChunkedResponseEntities {

  val streamedTextEndpointTest: Endpoint[Unit, Chunks[String]] =
    endpoint(get(path / "text"), ok(textChunksResponse))

  val streamedBytesEndpointTest: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "bytes"), ok(bytesChunksResponse))
}
