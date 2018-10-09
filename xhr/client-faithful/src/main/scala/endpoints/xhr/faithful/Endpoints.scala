package endpoints.xhr.faithful

import endpoints.algebra.Documentation
import endpoints.xhr
import faithful.{Future, Promise}

/**
  * Implements [[xhr.Endpoints]] by using faithful.
  *
  * @group interpreters
  */
trait Endpoints extends xhr.Endpoints {

  /** Maps `Result` to [[Future]] */
  type Result[A] = Future[A]

  def endpoint[A, B](
    request: Request[A],
    response: Response[B],
    summary: Documentation,
    description: Documentation,
    tags: List[String]
  ): Endpoint[A, B] =
    new Endpoint[A, B](request) {
      def apply(a: A) = {
        val promise = new Promise[B]()
        performXhr(request, response, a)(
          _.fold(promise.failure, promise.success),
          xhr => promise.failure(new Exception(xhr.responseText))
        )
        promise.future
      }
    }

}
