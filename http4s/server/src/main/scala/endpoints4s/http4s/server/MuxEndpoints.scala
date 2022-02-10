package endpoints4s.http4s.server

import cats.implicits._
import endpoints4s.{Decoder, Encoder, Invalid, Valid, algebra}
import org.http4s

import scala.util.control.NonFatal

/** Server interpreter of the algebra `MuxEndpoints` for http4s.
  *
  * @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with EndpointsWithCustomErrors {

  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ) {

    def implementedBy(handler: MuxHandler[Req, Resp])(implicit
        decoder: Decoder[Transport, Req],
        encoder: Encoder[Resp, Transport]
    ): Http4sRoute = handleEffect(req => Effect.pure(handler(req)))

    def implementedByEffect(muxHandlerEffect: MuxHandlerEffect[Effect, Req, Resp])(implicit
        decoder: Decoder[Transport, Req],
        encoder: Encoder[Resp, Transport]
    ): Http4sRoute = handleEffect(req => muxHandlerEffect(req))

    def handleEffect(handler: Req { type Response = Resp } => Effect[Resp])(implicit
        decoder: Decoder[Transport, Req],
        encoder: Encoder[Resp, Transport]
    ): Http4sRoute = {
      val f = { (http4sRequest: http4s.Request[Effect]) =>
        try {
          request
            .matches(http4sRequest)
            .map(_.flatMap {
              case Right(transport) =>
                try {
                  decoder.decode(transport) match {
                    case Valid(req) =>
                      handler(req.asInstanceOf[Req { type Response = Resp }])
                        .map(resp => response(encoder.encode(resp)))
                        .recoverWith { case NonFatal(t) =>
                          handleServerError(http4sRequest, t)
                        }
                    case inv: Invalid =>
                      handleClientErrors(http4sRequest, inv)
                  }
                } catch {
                  case NonFatal(t) => handleServerError(http4sRequest, t)
                }
              case Left(errorResponse) => errorResponse.pure[Effect]
            })
        } catch {
          case NonFatal(t) => Some(handleServerError(http4sRequest, t))
        }
      }
      Function.unlift(f)
    }

  }

  def muxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}

//#mux-handler-effect
/** A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req Request base type
  * @tparam Resp Response base type
  */
trait MuxHandlerEffect[F[_], Req <: algebra.MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): F[R]
}
//#mux-handler-effect

/** A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req Request base type
  * @tparam Resp Response base type
  */
trait MuxHandler[Req <: algebra.MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): R
}
