package endpoints.xhr.future

import endpoints.algebra.Documentation
import endpoints.xhr

import scala.concurrent.{Future, Promise}

/**
  * Implements [[xhr.Endpoints]] by using Scala’s [[Future]]s.
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
        val promise = Promise[B]()
        performXhr(request, response, a)(
          _.fold(exn => { promise.failure(exn); () }, b => { promise.success(b); () }),
          xhr => { promise.failure(new Exception(xhr.responseText)); () }
        )
        promise.future
      }
    }

}
