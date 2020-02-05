package sample

//#json-streaming
trait JsonStreamingExample
  extends endpoints.algebra.Endpoints
    with endpoints.algebra.ChunkedJsonEntities
    with endpoints.algebra.JsonEntitiesFromSchemas {

  val ticks = endpoint(get(path / "ticks"), ok(jsonChunksResponse[Unit]))

}
//#json-streaming
