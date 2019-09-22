package endpoints.play.server

import endpoints.algebra
import endpoints.algebra.{Decoder, Encoder, MuxRequest}
import play.api.mvc.Result

import scala.concurrent.Future

/**
  * @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with Endpoints {

  import playComponents.executionContext

  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ) {
    def implementedBy(
      handler: MuxHandler[Req, Resp]
    )(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): ToPlayHandler =
      toPlayHandler(req => Future.successful(handler(req)))

    def implementedByAsync(
      handler: MuxHandlerAsync[Req, Resp]
    )(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): ToPlayHandler =
      toPlayHandler(req => handler(req))

    def toPlayHandler(
      handler: Req { type Response = Resp } => Future[Resp]
    )(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): ToPlayHandler =
      header =>
        request.decode(header).map { bodyParser =>
          playComponents.defaultActionBuilder.async(bodyParser) { request =>
            handler(decoder.decode(request.body).right.get /* TODO Handle failure */.asInstanceOf[Req { type Response = Resp}])
              .map(resp => response(encoder.encode(resp)))
          }
        }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Transport => Result
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}


//#mux-handler-async
/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req Request base type
  * @tparam Resp Response base type
  */
trait MuxHandlerAsync[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): Future[R]
}
//#mux-handler-async

/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req Request base type
  * @tparam Resp Response base type
  */
trait MuxHandler[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): R
}
