package endpoints
package documented
package delegate

import endpoints.algebra.MuxRequest

/**
  * Interpreter for [[algebra.Endpoints]] that ignores information
  * related to documentation and delegates to another [[endpoints.algebra.Endpoints]]
  * interpreter.
  */
trait Endpoints
  extends algebra.Endpoints
    with Requests
    with Responses {

  val delegate: endpoints.algebra.Endpoints

  type Endpoint[A, B] = delegate.Endpoint[A, B]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    delegate.endpoint(request, response)

  type MuxEndpoint[Req <: MuxRequest, Resp, Transport] = delegate.MuxEndpoint[Req, Resp, Transport]

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = delegate.muxEndpoint(request, response)

}
