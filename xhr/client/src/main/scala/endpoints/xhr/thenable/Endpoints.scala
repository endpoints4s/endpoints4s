package endpoints.xhr.thenable

import endpoints.algebra.Documentation
import endpoints.xhr

import scala.scalajs.js

/**
  * Implements [[xhr.Endpoints]] by using JavaScript promises.
  */
trait Endpoints extends xhr.Endpoints {

  /** Maps a `Result` to a [[js.Thenable]] */
  type Result[A] = js.Thenable[A]

  def endpoint[A, B](request: Request[A], response: Response[B], summary: Documentation, description: Documentation, tags: List[String]): Endpoint[A, B] =
    new Endpoint[A, B](request) {
      def apply(a: A) =
        new js.Promise[B]((resolve, error) => {
          performXhr(request, response, a)(
            _.fold(exn => error(exn.getMessage), b => resolve(b)),
            xhr => error(xhr.responseText)
          )
        })
    }

}
