package endpoints4s.xhr.faithful

import endpoints4s.xhr
import faithful.Future
import faithful.Promise

/** Implements [[xhr.Endpoints]] by using faithful.
  *
  * @group interpreters
  */
trait Endpoints extends xhr.Endpoints {

  /** Maps `Result` to `Future` */
  abstract class Result[A](val future: Future[A]) {
    def abort(): Unit
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    new Endpoint[A, B](request, response) {

      def apply(a: A): Result[B] = {
        val promise = new Promise[B]()
        val jsAbort = performXhr(this.request, this.response, a)(
          _.fold(promise.failure, promise.success),
          promise.failure
        )
        new Result(promise.future) {
          def abort(): Unit = jsAbort()
        }
      }
    }

  override def mapEndpointRequest[A, B, C](
      currentEndpoint: Endpoint[A, B],
      func: Request[A] => Request[C]
  ): Endpoint[C, B] = endpoint(func(currentEndpoint.request), currentEndpoint.response)

  override def mapEndpointResponse[A, B, C](
      currentEndpoint: Endpoint[A, B],
      func: Response[B] => Response[C]
  ): Endpoint[A, C] = endpoint(currentEndpoint.request, func(currentEndpoint.response))

  override def mapEndpointDocs[A, B](
      currentEndpoint: Endpoint[A, B],
      func: EndpointDocs => EndpointDocs
  ): Endpoint[A, B] = currentEndpoint

}
