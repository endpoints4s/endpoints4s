package endpoints4s.fetch.thenable

import endpoints4s.fetch

import scala.scalajs.js

trait Endpoints extends fetch.Endpoints with EndpointsWithCustomErrors

trait EndpointsWithCustomErrors extends fetch.EndpointsWithCustomErrors {

  abstract class Result[A](val thenable: js.Thenable[A]) {
    def abort(): Unit
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = new Endpoint[A, B](request, response) {
    def apply(a: A) = {
      var jsAbort: js.Function0[Unit] = null
      val promise =
        new js.Promise[B]((resolve, error) => {
          jsAbort = performFetch(this.request, this.response, a)(
            _.fold(exn => { error(exn.getMessage); () }, b => { resolve(b); () }),
            throwable => { error(throwable.toString); () }
          )
        })
      new Result(promise) {
        def abort(): Unit = jsAbort()
      }
    }
  }
}
