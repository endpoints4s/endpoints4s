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
    new Endpoint[A, B](request) {
      def apply(a: A) =
        new js.Promise[B]((resolve, error) => {
          performXhr(request, response, a)(
            _.fold(exn => error(exn.getMessage), b => resolve(b)): Unit,
            xhr => error(xhr.responseText): Unit
          )
        })
    }

}
