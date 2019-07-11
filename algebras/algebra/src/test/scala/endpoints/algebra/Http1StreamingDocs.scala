package endpoints.algebra

trait Http1StreamingDocs extends Http1Streaming {

//#chunked-endpoint
  val logo: ChunkedEndpoint[Unit, Array[Byte]] =
    chunkedEndpoint(get(path / "logo.png"), bytesChunks())
//#chunked-endpoint

//#websocket-endpoint
  val chat: WebSocketEndpoint[Unit, String, String] =
    webSocketEndpoint(path / "chat", textChunks(), textChunks())
//#websocket-endpoint

}
