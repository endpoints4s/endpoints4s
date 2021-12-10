package endpoints4s.algebra

trait ChunkedJsonEntitiesTestApi
    extends ChunkedJsonRequestEntitiesTestApi
    with ChunkedJsonResponseEntitiesTestApi

trait ChunkedJsonRequestEntitiesTestApi
    extends EndpointsTestApi
    with ChunkedJsonRequestEntities
    with JsonEntitiesFromCodecs
    with CounterCodec {

  val streamedJsonUpload: Endpoint[Chunks[Counter], String] =
    endpoint(
      post(path / "counter-values", jsonChunksRequest[Counter](newLineDelimiterRequestFraming)),
      ok(textResponse)
    )
}

trait ChunkedJsonResponseEntitiesTestApi
    extends EndpointsTestApi
    with ChunkedJsonResponseEntities
    with JsonEntitiesFromCodecs
    with CounterCodec {

  val streamedEndpointTest: Endpoint[Unit, Chunks[Counter]] =
    endpoint(
      get(path / "notifications"),
      ok(jsonChunksResponse[Counter](newLineDelimiterResponseFraming))
    )

  val streamedEndpointErrorTest: Endpoint[Unit, Chunks[Counter]] =
    endpoint(
      get(path / "notifications" / "error"),
      ok(jsonChunksResponse[Counter](newLineDelimiterResponseFraming))
    )
}
