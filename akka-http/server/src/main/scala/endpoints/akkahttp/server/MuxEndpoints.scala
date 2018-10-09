package endpoints
package akkahttp.server

import akka.http.scaladsl.server.{Directives, Route}
import endpoints.algebra.{Decoder, Encoder, MuxRequest}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Extends the [[Endpoints]] interpreter with [[algebra.MuxEndpoints]]
  * support.
  *
  * @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with Endpoints {

  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](request: Request[Transport], response: Response[Transport]) {

    def implementedBy(handler: MuxHandler[Req, Resp])(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): Route = handleAsync(req => Future.successful(handler(req)))

    def implementedByAsync(handler: MuxHandlerAsync[Req, Resp])(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): Route = handleAsync(req => handler(req))

    private def handleAsync(handler: Req {type Response = Resp} => Future[Resp])(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): Route =
      request { request =>
        Directives.onComplete(handler(decoder.decode(request).right.get /* TODO Handle failure */ .asInstanceOf[Req {type Response = Resp}])) {
          case Success(result) => response(encoder.encode(result))
          case Failure(ex) => Directives.complete(ex)
        }
      }

  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}

/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req  Request base type
  * @tparam Resp Response base type
  */
trait MuxHandlerAsync[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): Future[R]
}

/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req  Request base type
  * @tparam Resp Response base type
  */
trait MuxHandler[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): R
}
