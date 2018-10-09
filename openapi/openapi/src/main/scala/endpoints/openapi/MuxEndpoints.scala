package endpoints.openapi

import endpoints.algebra.MuxRequest
import endpoints.algebra

/**
  * @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with Endpoints {

  type MuxEndpoint[Req <: MuxRequest, Resp, Transport] = DocumentedEndpoint

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = endpoint(request, response)

}