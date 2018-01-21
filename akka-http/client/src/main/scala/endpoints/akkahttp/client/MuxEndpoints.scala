package endpoints
package akkahttp.client

import endpoints.algebra.{Decoder, Encoder, MuxRequest}

import scala.concurrent.Future

trait MuxEndpoints extends algebra.MuxEndpoints { self: Endpoints =>


  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ) {
    def apply(
      req: Req
    )(implicit
      encoder: Encoder[Req, Transport],
      decoder: Decoder[Transport, Resp]
    ): Future[req.Response] =
      request(encoder.encode(req)).flatMap { resp =>
        response(resp).flatMap { t =>
          futureFromEither(t).flatMap(tt =>
            futureFromEither(decoder.decode(tt)).map(_.asInstanceOf[req.Response])
          )
        }
      }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
