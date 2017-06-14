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
    with DocumentedUrls
    with Methods {

  val delegate: algebra.Endpoints

  type RequestHeaders[A] = delegate.RequestHeaders[A]

  def emptyHeaders: RequestHeaders[Unit] = delegate.emptyHeaders

  type Request[A] = delegate.Request[A]

  type RequestEntity[A] = delegate.RequestEntity[A]

  def emptyRequest: RequestEntity[Unit] = delegate.emptyRequest

  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    delegate.request(method, url, entity, headers)

  type Response[A] = delegate.Response[A]

  def emptyResponse(description: String): Response[Unit] = delegate.emptyResponse

  type Endpoint[A, B] = delegate.Endpoint[A, B]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    delegate.endpoint(request, response)

  type MuxEndpoint[Req <: MuxRequest, Resp, Transport] = delegate.MuxEndpoint[Req, Resp, Transport]

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = delegate.muxEndpoint(request, response)

}
