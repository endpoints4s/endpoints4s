package endpoints4s.http4s.client

import cats.implicits._
import cats.effect.Resource
import endpoints4s.{Decoder, Encoder, Invalid, Valid, algebra}

/** Client interpreter of the algebra `MuxEndpoints` for http4s.
  *
  * @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with EndpointsWithCustomErrors {

  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ) {
    def send(
        req: Req
    )(implicit
        encoder: Encoder[Req, Transport],
        decoder: Decoder[Transport, Resp]
    ): Resource[Effect, req.Response] =
      Resource.eval(request(encoder.encode(req))).flatMap { http4sRequest =>
        client
          .run(http4sRequest)
          .evalMap(res =>
            decodeResponse(response, res).flatMap { responseEntity =>
              responseEntity(res).flatMap { transport =>
                decoder.decode(transport) match {
                  case Valid(resp) => effect.pure(resp.asInstanceOf[req.Response])
                  case Invalid(errors) =>
                    effect.raiseError[req.Response](new Exception(errors.mkString(". ")))
                }
              }
            }
          )
      }
  }

  def muxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
