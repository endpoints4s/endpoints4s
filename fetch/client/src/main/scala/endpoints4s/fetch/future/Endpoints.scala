package endpoints4s.fetch.future

import endpoints4s.fetch

import scala.concurrent.Future
import scala.concurrent.Promise

trait Endpoints extends fetch.Endpoints with EndpointsWithCustomErrors

trait EndpointsWithCustomErrors extends fetch.EndpointsWithCustomErrors {

  type Result[A] = Future[A]

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = { a =>
    val promise = Promise[B]()
    performFetch(request, response, a)(
      _.fold(
        exn => { promise.failure(exn); () },
        b => {
          promise.success(b); ()
        }
      ),
      ex => { promise.failure(ex); () }
    )
    promise.future
  }
}
