package endpoints4s.fetch.future

import endpoints4s.fetch

import scala.concurrent.Future
import scala.concurrent.Promise

trait Endpoints extends fetch.Endpoints with EndpointsWithCustomErrors

trait EndpointsWithCustomErrors extends fetch.EndpointsWithCustomErrors {

  abstract class Result[A](val future: Future[A]) {
    def abort(): Unit
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = new Endpoint[A, B](request, response) {
    def apply(a: A) = {
      val promise = Promise[B]()
      val jsAbort = performFetch(this.request, this.response, a)(
        _.fold(
          exn => { promise.failure(exn); () },
          b => { promise.success(b); () }
        ),
        throwable => { promise.failure(throwable); () }
      )
      new Result(promise.future) {
        def abort(): Unit = jsAbort()
      }
    }
  }
}
