package endpoints4s.algebra

import endpoints4s.Tupler

trait WebSockets extends WebsocketClientStream with WebsocketServerStream with Urls {

  type WebSocket[A, B]

  def webSocket[URLC, CSC, In, Out](
      url: Url[URLC],
      clientStream: WebsocketClientStream[CSC] = emptyWebsocketClientStream,
      serverStream: WebsocketServerStream[Out] = emptyWebsocketServerStream
  )(implicit
      tupler: Tupler.Aux[URLC, CSC, In]
  ): WebSocket[In, Out]
}

trait WebSocketsWithUpgrade
    extends WebSockets
    with WebSocketUpgradeRequest
    with WebSocketUpgradeResponse {

  type WebSocketWithUpgrade[A, B]

  def webSocketWithUpgrade[URLC, CSC, URHC, SSS, URHS, URLCAndCSCTupled, In, Out](
      url: Url[URLC],
      clientStream: WebsocketClientStream[CSC] = emptyWebsocketClientStream,
      upgradeRequestHeaders: RequestHeaders[URHC] = emptyRequestHeaders,
      serverStream: WebsocketServerStream[SSS] = emptyWebsocketServerStream,
      upgradeResponseHeaders: RequestHeaders[URHS] = emptyRequestHeaders
  )(
      tuplerURLCCSC: Tupler.Aux[URLC, CSC, URLCAndCSCTupled],
      tuplerURLCAndCSCTupledURHC: Tupler.Aux[URLCAndCSCTupled, URHC, In],
      tuplerSSSURHS: Tupler.Aux[SSS, URHS, Out]
  ): WebSocketWithUpgrade[In, Out]
}

trait WebsocketClientStream {

  type WebsocketClientStream[A]

  def emptyWebsocketClientStream: WebsocketClientStream[Unit]
}

trait WebsocketServerStream {

  type WebsocketServerStream[A]

  def emptyWebsocketServerStream: WebsocketServerStream[Unit]
}

trait WebSocketUpgradeRequest {

  type RequestHeaders[A]

  def emptyRequestHeaders: RequestHeaders[Unit]
}

trait WebSocketUpgradeResponse {

  type ResponseHeaders[A]

  def emptyResponseHeaders: ResponseHeaders[Unit]
}
