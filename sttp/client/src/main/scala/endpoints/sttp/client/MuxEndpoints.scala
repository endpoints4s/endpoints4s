package endpoints
package sttp.client

import com.softwaremill.sttp
import endpoints.algebra.{Decoder, Encoder, MuxRequest}

import scala.language.higherKinds

/**
  * @group interpreters
  */
trait MuxEndpoints[R[_]] extends algebra.Endpoints { self: Endpoints[R] =>

  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](request: Request[Transport], response: Response[Transport]) {

    def apply(req: Req)(implicit encoder: Encoder[Req, Transport], decoder: Decoder[Transport, Resp]): R[req.Response] = {
      val sttpRequest: sttp.Request[response.ReceivedBody, Nothing] = request(encoder.encode(req)).response(response.responseAs)
      val result = self.backend.send(sttpRequest)
      self.backend.responseMonad.flatMap(result) { res =>
        val transportR: R[Transport] = response.validateResponse(res)
        self.backend.responseMonad.flatMap(transportR) { transport =>
          decoder.decode(transport) match {
            case Right(r) => self.backend.responseMonad.unit(r.asInstanceOf[req.Response])
            case Left(exception) => self.backend.responseMonad.error(exception)
          }
        }
      }
    }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
