package endpoints4s.algebra

import endpoints4s._

/** Algebra for operations that a middleware can perform on requests.
  *
  * @see [[Middlewares]]
  */
trait RequestMiddlewares extends Requests {

  def addRequestHeaders[A, H](
      request: Request[A],
      headers: RequestHeaders[H]
  )(implicit tupler: Tupler[A, H]): Request[tupler.Out]

  def addRequestQueryString[A, Q, Out](
      request: Request[A],
      qs: QueryString[Q]
  )(implicit tupler: Tupler[A, Q]): Request[tupler.Out]

  implicit class RequestMiddlewareOps[A](request: Request[A]) {

    def withHeaders[H](headers: RequestHeaders[H])(implicit
        tupler: Tupler[A, H]
    ): Request[tupler.Out] =
      addRequestHeaders(request, headers)

    def withQueryString[Q](qs: QueryString[Q])(implicit
        tupler: Tupler[A, Q]
    ): Request[tupler.Out] =
      addRequestQueryString(request, qs)
  }
}

/** Algebra for operations that a middleware can perform on responses.
  *
  * @see [[Middlewares]]
  */
trait ResponseMiddlewares extends Responses { this: Errors =>

  def addResponseHeaders[A, H](
      response: Response[A],
      headers: ResponseHeaders[H]
  )(implicit tupler: Tupler[A, H]): Response[tupler.Out]

  implicit class ResponseMiddlewareOps[A](response: Response[A]) {

    def withHeaders[H](headers: ResponseHeaders[H])(implicit
        tupler: Tupler[A, H]
    ): Response[tupler.Out] =
      addResponseHeaders(response, headers)
  }
}

/** Algebra to modify an endpoint.
  *
  * This can be used for defining an Authentication or tracing middleware for example.
  */
trait Middlewares
    extends EndpointsWithCustomErrors
    with RequestMiddlewares
    with ResponseMiddlewares {

  def mapEndpointRequest[A, B, C](
      endpoint: Endpoint[A, B],
      f: Request[A] => Request[C]
  ): Endpoint[C, B]
  def mapEndpointResponse[A, B, C](
      endpoint: Endpoint[A, B],
      f: Response[B] => Response[C]
  ): Endpoint[A, C]
  def mapEndpointDocs[A, B](
      endpoint: Endpoint[A, B],
      f: EndpointDocs => EndpointDocs
  ): Endpoint[A, B]

  implicit class EndpointMiddlewareOps[A, B](endpoint: Endpoint[A, B]) {
    def mapRequest[C](f: Request[A] => Request[C]): Endpoint[C, B] =
      mapEndpointRequest(endpoint, f)
    def mapResponse[C](f: Response[B] => Response[C]): Endpoint[A, C] =
      mapEndpointResponse(endpoint, f)
    def mapDocs(f: EndpointDocs => EndpointDocs): Endpoint[A, B] =
      mapEndpointDocs(endpoint, f)
  }
}
