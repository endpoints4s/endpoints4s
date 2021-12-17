package endpoints4s.xhr.future

import endpoints4s.xhr

import scala.concurrent.Future

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
  abstract class Result[A](val future: Future[A]) {
    def abort(): Unit
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    new Endpoint[A, B](request, response) {

      def apply(a: A) = {
        val (value, jsAbort) = performXhr(this.request, this.response, a)
        new Result(value.toFuture) { def abort() = jsAbort(()) }
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
