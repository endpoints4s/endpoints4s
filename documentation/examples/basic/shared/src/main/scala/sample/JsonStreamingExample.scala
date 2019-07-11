package sample

//#json-streaming
trait JsonStreamingExample
  extends endpoints.algebra.Http1JsonStreaming
    with endpoints.algebra.JsonSchemaEntities {

  val ticks = chunkedEndpoint(get(path / "ticks"), jsonChunks[Unit]())

}
//#json-streaming
