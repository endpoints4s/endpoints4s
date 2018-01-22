package endpoints.documented.delegate

import endpoints.algebra.MuxRequest

trait MuxEndpoints extends endpoints.documented.algebra.MuxEndpoints with Endpoints {

  val delegate: endpoints.algebra.MuxEndpoints

  type MuxEndpoint[Req <: MuxRequest, Resp, Transport] = delegate.MuxEndpoint[Req, Resp, Transport]

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = delegate.muxEndpoint(request, response)

}
