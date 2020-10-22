package endpoints4s.xhr.future

import endpoints4s.xhr

import scala.concurrent.{Future, Promise}

/** Implements [[xhr.Endpoints]] by using Scala’s `Futures`, and uses [[endpoints4s.algebra.BuiltInErrors]]
  * to model client and server errors.
  *
  * @group interpreters
  */
trait Endpoints extends xhr.Endpoints with EndpointsWithCustomErrors

/** Implements [[xhr.Endpoints]] by using Scala’s `Future`s.
  * @group interpreters
  */
trait EndpointsWithCustomErrors extends xhr.EndpointsWithCustomErrors {

  /** Maps `Result` to `Future` */
  type Result[A] = Future[A]

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    new Endpoint[A, B](request) {
      def apply(a: A) = {
        val promise = Promise[B]()
        performXhr(request, response, a)(
          _.fold(
            exn => { promise.failure(exn); () },
            b => {
              promise.success(b); ()
            }
          ),
          xhr => { promise.failure(new Exception(xhr.responseText)); () }
        )
        promise.future
      }
    }

}
