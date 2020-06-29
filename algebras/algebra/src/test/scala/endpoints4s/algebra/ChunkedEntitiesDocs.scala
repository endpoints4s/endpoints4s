package endpoints4s.algebra

trait ChunkedEntitiesDocs extends ChunkedEntities {

//#streamed-endpoint
  val logo: Endpoint[Unit, Chunks[Array[Byte]]] =
    endpoint(get(path / "logo.png"), ok(bytesChunksResponse))
//#streamed-endpoint

}

//#json-streaming
trait JsonStreamingExample
    extends endpoints4s.algebra.Endpoints
    with endpoints4s.algebra.ChunkedJsonEntities
    with endpoints4s.algebra.JsonEntitiesFromSchemas {

  val ticks = endpoint(get(path / "ticks"), ok(jsonChunksResponse[Unit]))

}
//#json-streaming
