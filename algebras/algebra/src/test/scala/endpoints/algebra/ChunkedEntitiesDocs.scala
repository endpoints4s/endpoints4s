package endpoints.algebra

trait ChunkedEntitiesDocs extends ChunkedEntities {

//#streamed-endpoint
  val logo: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "logo.png"), ok(bytesChunksResponse))
//#streamed-endpoint

}

//#json-streaming
trait JsonStreamingExample
  extends endpoints.algebra.Endpoints
    with endpoints.algebra.ChunkedJsonEntities
    with endpoints.algebra.JsonEntitiesFromSchemas {

  val ticks = endpoint(get(path / "ticks"), ok(jsonChunksResponse[Unit]))

}
//#json-streaming
