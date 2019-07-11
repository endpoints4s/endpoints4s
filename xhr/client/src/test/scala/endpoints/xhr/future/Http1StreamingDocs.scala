package endpoints.xhr.future

import endpoints.algebra
import endpoints.xhr.Http1Streaming

trait Http1StreamingDocs extends algebra.Http1StreamingDocs with Http1Streaming {

  //#invocation
  val chunkedResponse: ChunkedResponse[Array[Byte]] = logo(())
  // And then:
  chunkedResponse.onChunk(bytes => println(s"Received ${bytes.length} bytes"))
  chunkedResponse.onError(e => println(s"An error occurred when downloading the file: $e"))
  chunkedResponse.onComplete(() => println("Download finished"))
  // If needed, the download can be cancelled at any time with the following call:
  chunkedResponse.abort()
  //#invocation

  //#websocket-invocation
  val webSocket: WebSocket[String, String] = chat(())
  // And then:
  webSocket.onMessage { message =>
    println(s"Received: $message")
    val response = message.toUpperCase
    println(s"Sending: $response")
    webSocket.send(response)
  }
  webSocket.onError(error => println(s"Error: $error"))
  webSocket.onComplete(() => println("Communication ended"))
  // If needed, the conversation can be terminated with the following call:
  webSocket.close()
  //#websocket-invocation

}
