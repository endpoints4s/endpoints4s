package endpoints4s.fetch.thenable

import endpoints4s.fetch

import scala.scalajs.js

trait Endpoints extends fetch.Endpoints with EndpointsWithCustomErrors

trait EndpointsWithCustomErrors extends fetch.EndpointsWithCustomErrors {

  type Result[A] = js.Thenable[A]

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = { a =>
    new js.Promise[B]((resolve, error) => {
      performFetch(request, response, a)(
        _.fold(exn => error(exn.getMessage), b => resolve(b)): Unit,
        ex => error(ex): Unit
      )
    })
  }
}
