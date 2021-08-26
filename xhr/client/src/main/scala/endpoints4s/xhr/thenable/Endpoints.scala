package endpoints4s.xhr.thenable

import endpoints4s.xhr

import scala.scalajs.js

/** Implements [[xhr.Endpoints]] by using JavaScript promises, and
  * [[endpoints4s.algebra.BuiltInErrors]] to model client and server errors.
  *
  * @group interpreters
  */
trait Endpoints extends xhr.Endpoints with EndpointsWithCustomErrors

/** Implements [[xhr.Endpoints]] by using JavaScript promises
  * @group interpreters
  */
trait EndpointsWithCustomErrors extends xhr.EndpointsWithCustomErrors {

  /** Maps a `Result` to a `js.Thenable` */
  type Result[A] = js.Thenable[A]

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    new Endpoint[A, B](request, response) {

      def apply(a: A) =
        new js.Promise[B]((resolve, error) => {
          performXhr(this.request, this.response, a)(
            _.fold(exn => error(exn.getMessage), b => resolve(b)): Unit,
            xhr => error(xhr.responseText): Unit
          )
        })
    }

  override def mapEndpointRequest[A, B, C](
      e: Endpoint[A, B],
      func: Request[A] => Request[C]
  ): Endpoint[C, B] = endpoint(func(e.request), e.response)

  override def mapEndpointResponse[A, B, C](
      e: Endpoint[A, B],
      func: Response[B] => Response[C]
  ): Endpoint[A, C] = endpoint(e.request, func(e.response))

  override def mapEndpointDocs[A, B](
      endpoint: Endpoint[A, B],
      func: EndpointDocs => EndpointDocs
  ): Endpoint[A, B] = endpoint

}
