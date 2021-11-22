package endpoints4s.openapi

import endpoints4s.algebra.MuxRequest
import endpoints4s.algebra

/** @group interpreters
  */
trait MuxEndpoints extends algebra.MuxEndpoints with EndpointsWithCustomErrors {

  type MuxEndpoint[Req <: MuxRequest, Resp, Transport] = Endpoint[Req, Resp]

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = endpoint[Req, Resp](request, response)

}
