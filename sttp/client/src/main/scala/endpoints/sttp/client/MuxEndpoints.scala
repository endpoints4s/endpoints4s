package endpoints
package sttp.client

import com.softwaremill.sttp
import endpoints.algebra.{Decoder, Encoder, MuxRequest}

import scala.language.higherKinds

trait MuxEndpoints[R[_]] extends algebra.Endpoints { self: Endpoints[R] =>

  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](request: Request[Transport], response: Response[Transport]) {

    def apply(req: Req)(implicit encoder: Encoder[Req, Transport], decoder: Decoder[Transport, Resp]): R[Either[String, req.Response]] = {
      val sttpRequest: sttp.Request[response.RB, Nothing] = request(encoder.encode(req)).response(response.responseAs)
      val result = self.backend.send(sttpRequest)
      self.backend.responseMonad.map(result)(res => response.validateResponse(res).right.flatMap { t =>
        decoder.decode(t).left.map(ex => s"Could not decode transport: $ex").asInstanceOf[Either[String, req.Response]]
      })
    }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}
