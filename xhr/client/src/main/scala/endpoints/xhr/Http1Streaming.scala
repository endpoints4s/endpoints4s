package endpoints.xhr

import endpoints.algebra
import endpoints.algebra.Documentation
import org.scalajs.dom
import org.scalajs.dom.{ErrorEvent, MessageEvent, XMLHttpRequest, WebSocket => JsWebSocket}

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array}

/**
  * Interpreter for [[algebra.Http1Streaming]] that uses XMLHttpRequest and WebSocket from
  * the DOM API.
  *
  * @group interpreters
  */
trait Http1Streaming extends algebra.Http1Streaming with Endpoints {

  abstract class Chunks[A] {
    type Serialized
    def xhrResponseType: String
    def nextXhrChunk(xhr: XMLHttpRequest, previousContentLength: Int): Option[(Either[Throwable, A], Int)]
    def setWebSocketBinaryType(ws: JsWebSocket): Unit
    def sendWebSocketMessage(ws: JsWebSocket, a: A): Unit
    def decodeWebSocketMessage(message: MessageEvent): Either[Throwable, A]
  }

  type ChunkedEndpoint[A, B] = js.Function1[A, ChunkedResponse[B]]

  trait ChunkedResponse[A] {
    def onChunk(f: A => Unit): Unit
    def onError(f: Throwable => Unit): Unit
    def onComplete(f: () => Unit): Unit
    /** Close the connection */
    def abort(): Unit
  }

  def chunkedEndpoint[A, B](
    req: Request[A],
    responseChunks: Chunks[B],
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): ChunkedEndpoint[A, B] = { a =>
    val (xhr, maybeEntity) = req(a)
    xhr.responseType = responseChunks.xhrResponseType
    xhr.send(maybeEntity.orNull)
    var previousContentLength = 0
    var errorHandler: Throwable => Unit = e => ()
    xhr.onerror = e => errorHandler(new Exception(s"XMLHttpRequest error: ${e.message}"))
    new ChunkedResponse[B] {
      def onChunk(f: B => Unit): Unit = xhr.onprogress = _ => {
        responseChunks.nextXhrChunk(xhr, previousContentLength).foreach {
          case (maybeChunk, length) =>
            maybeChunk.fold(errorHandler, f)
            previousContentLength = length
        }
      }
      def onError(f: Throwable => Unit): Unit =
        errorHandler = f
      def onComplete(f: () => Unit): Unit =
        xhr.onload = _ => f()
      def abort(): Unit = xhr.abort()
    }
  }

  type WebSocketEndpoint[A, B, C] = js.Function1[A, WebSocket[B, C]]

  /**
    * @tparam A Type of messages that can be sent through the WebSocket
    * @tparam B Type of messages that are received through the WebSocket
    */
  trait WebSocket[A, B] {
    def send(a: A): Unit
    def onMessage(f: B => Unit): Unit
    def onError(f: Throwable => Unit): Unit
    def onComplete(f: () => Unit): Unit
    def close(): Unit
  }

  def webSocketEndpoint[A, B, C, S](
    url: Url[A],
    requestChunks: Chunks[B] { type Serialized = S },
    responseChunks: Chunks[C] { type Serialized = S },
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): WebSocketEndpoint[A, B, C] =
    a => {
      val protocol = if (dom.document.location.protocol == "http") "ws" else "wss"
      val host = dom.document.location.host
      val wsUrl = s"$protocol://$host${url.encode(a)}"
      val ws = new JsWebSocket(wsUrl)
      responseChunks.setWebSocketBinaryType(ws)
      var errorHandler: Throwable => Unit = e => ()
      ws.onerror = e => errorHandler(new Exception(s"WebSocket error: ${e.asInstanceOf[ErrorEvent].message}"))
      new WebSocket[B, C] {
        def send(b: B): Unit = requestChunks.sendWebSocketMessage(ws, b)
        def onMessage(f: C => Unit): Unit = ws.onmessage = m => {
          responseChunks.decodeWebSocketMessage(m).fold(errorHandler, f)
        }
        def onError(f: Throwable => Unit): Unit = errorHandler = f
        def onComplete(f: () => Unit): Unit = ws.onclose = _ => f()
        def close(): Unit = ws.close()
      }
    }

  def bytesChunks(docs: Documentation): Chunks[Array[Byte]] { type Serialized = Array[Byte] } =
    new Chunks[Array[Byte]] {
      type Serialized = Array[Byte]
      def xhrResponseType: String = "arraybuffer"
      def nextXhrChunk(xhr: XMLHttpRequest, previousContentLength: Int): Option[(Either[Throwable, Array[Byte]], Int)] = {
        val response = xhr.response.asInstanceOf[ArrayBuffer]
        if (response.byteLength > previousContentLength) {
          val chunk = response.slice(previousContentLength)
          Some((Right(new Int8Array(chunk).toArray), response.byteLength))
        } else None
      }
      def setWebSocketBinaryType(ws: JsWebSocket): Unit = ws.binaryType = "arraybuffer"
      def sendWebSocketMessage(ws: JsWebSocket, bytes: Array[Byte]): Unit = ws.send(new Int8Array(js.Array(bytes: _*)).buffer)
      def decodeWebSocketMessage(message: MessageEvent): Either[Throwable, Array[Byte]] = {
        val chunk = message.data.asInstanceOf[ArrayBuffer]
        Right(new Int8Array(chunk).toArray)
      }
    }

  def textChunks(docs: Documentation): Chunks[String] { type Serialized = String } =
    new Chunks[String] {
      type Serialized = String
      def xhrResponseType: String = "text"
      def nextXhrChunk(xhr: XMLHttpRequest, previousContentLength: Int): Option[(Either[Throwable, String], Int)] = {
        val response = xhr.response.asInstanceOf[String]
        if (response.length > previousContentLength) {
          val chunk = response.substring(previousContentLength)
          Some((Right(chunk), response.length))
        } else None
      }
      def setWebSocketBinaryType(ws: JsWebSocket): Unit = () // We deal with text frames only
      def sendWebSocketMessage(ws: JsWebSocket, s: String): Unit = ws.send(s)
      def decodeWebSocketMessage(message: MessageEvent): Either[Throwable, String] =
        Right(message.data.asInstanceOf[String])
    }
}

/**
  * @group interpreters
  */
trait Http1JsonStreaming extends algebra.Http1JsonStreaming with algebra.JsonEntitiesFromCodec with Http1Streaming {

  def jsonChunks[A](docs: Documentation)(implicit jsonCodec: JsonCodec[A]): Chunks[A] { type Serialized = String } =
    new Chunks[A] {
      type Serialized = String
      val codec = jsonCodecToCodec(jsonCodec)
      def xhrResponseType = "text"
      def nextXhrChunk(xhr: XMLHttpRequest, previousContentLength: Int) = {
        val response = xhr.response.asInstanceOf[String]
        if (response.length > previousContentLength) {
          val chunk = response.substring(previousContentLength)
          Some((codec.decode(chunk), response.length))
        } else None
      }
      def sendWebSocketMessage(ws: JsWebSocket, a: A): Unit = {
        ws.send(codec.encode(a))
      }
      def setWebSocketBinaryType(ws: JsWebSocket): Unit = () // In our case we only expect text frames
      def decodeWebSocketMessage(message: MessageEvent): Either[Throwable, A] = {
        val chunk = message.data.asInstanceOf[Serialized]
        codec.decode(chunk)
      }
    }

}
