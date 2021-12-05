package sample

//#json-streaming
trait JsonStreamingExample
    extends endpoints4s.algebra.Endpoints
    with endpoints4s.algebra.ChunkedJsonEntities
    with endpoints4s.algebra.JsonEntitiesFromSchemas
    with endpoints4s.algebra.NewLineResponseChunkCodec {

  val ticks = endpoint(get(path / "ticks"), ok(jsonChunksResponse[Unit](newLineResponseChunkCodec)))

}
//#json-streaming
