package endpoints4s.fetch.thenable

import endpoints4s.fetch

import scala.scalajs.js

trait Endpoints extends fetch.Endpoints with EndpointsWithCustomErrors

trait EndpointsWithCustomErrors
    extends fetch.EndpointsWithCustomErrors
    with ThenableBasedFutureLike {

  abstract class Result[A](val thenable: js.Thenable[A]) {
    val abort: js.Function0[Unit]
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = new Endpoint[A, B](request, response) {
    def apply(a: A) = {
      val (value, jsAbort) = performFetch(this.request, this.response, a)
      new Result(value) { val abort = jsAbort }
    }
  }
}
