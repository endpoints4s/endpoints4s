package endpoints4s.xhr.faithful

import endpoints4s.xhr
import faithful.{Future, Promise}

import scala.scalajs.js
import scala.scalajs.js.|

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
        val (value, jsAbort) = performXhr(this.request, this.response, a)
        value.`then`(
          (b: B) => promise.success(b): Unit | js.Thenable[Unit],
          js.defined((e: Any) => {
            e match {
              case th: Throwable => promise.failure(th)
              case _             => promise.failure(js.JavaScriptException(e))
            }
            (): Unit | js.Thenable[Unit]
          }): js.UndefOr[
            js.Function1[Any, Unit | js.Thenable[Unit]]
          ]
        )

        new Result(promise.future) { def abort() = jsAbort(()) }
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
