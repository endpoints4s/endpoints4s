package endpoints
package delegate

import endpoints.algebra.MuxRequest

/**
  * Interpreter for [[algebra.DocumentedEndpoints]] that ignores information
  * related to documentation and delegates to another [[algebra.Endpoints]]
  * interpreter.
  */
trait DocumentedEndpoints
  extends algebra.DocumentedEndpoints
    with DocumentedRequests
    with DocumentedResponses {

  val delegate: algebra.Endpoints

  type Endpoint[A, B] = delegate.Endpoint[A, B]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    delegate.endpoint(request, response)

  type MuxEndpoint[Req <: MuxRequest, Resp, Transport] = delegate.MuxEndpoint[Req, Resp, Transport]

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = delegate.muxEndpoint(request, response)

}
