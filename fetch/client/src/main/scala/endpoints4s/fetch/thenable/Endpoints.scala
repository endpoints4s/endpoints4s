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
      val (value, jsAbort) = performFetch(this.request, this.response, a)
      new Result(value) { def abort() = jsAbort(()) }
    }
  }
}
