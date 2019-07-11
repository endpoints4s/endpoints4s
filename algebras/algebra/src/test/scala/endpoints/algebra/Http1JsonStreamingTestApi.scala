package endpoints.algebra

trait Http1JsonStreamingTestApi extends EndpointsTestApi with Http1JsonStreaming with JsonEntitiesFromCodec {

  case class Counter(value: Int)

  implicit def counterCodec: JsonCodec[Counter]

  val chunkedEndpointTest: ChunkedEndpoint[Unit, Counter] =
    chunkedEndpoint(get(path / "notifications"), jsonChunks[Counter]())

  val webSocketEndpointTest: WebSocketEndpoint[Unit, Counter, Counter] =
    webSocketEndpoint(path / "ping-pong", jsonChunks[Counter](), jsonChunks[Counter]())

}
