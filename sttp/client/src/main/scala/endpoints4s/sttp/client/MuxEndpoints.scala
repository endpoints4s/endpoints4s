package endpoints4s
package sttp.client

import _root_.sttp.client.{asStringAlways, Request => SRequest}
import endpoints4s.algebra.MuxRequest

/** @group interpreters
  */
trait MuxEndpoints[R[_]] extends algebra.Endpoints {
  self: EndpointsWithCustomErrors[R] =>

  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ) {

    def apply(req: Req)(implicit
        encoder: Encoder[Req, Transport],
        decoder: Decoder[Transport, Resp]
    ): R[req.Response] = {
      val sttpRequest: SRequest[String, Nothing] =
        request(encoder.encode(req)).response(asStringAlways)
      val result = self.backend.send(sttpRequest)
      self.backend.responseMonad.flatMap(result) { res =>
        self.backend.responseMonad.flatMap(decodeResponse(response, res)) { transport =>
          decoder.decode(transport) match {
            case Valid(r) =>
              self.backend.responseMonad.unit(r.asInstanceOf[req.Response])
            case Invalid(errors) =>
              self.backend.responseMonad
                .error(new Exception(errors.mkString(". ")))
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
