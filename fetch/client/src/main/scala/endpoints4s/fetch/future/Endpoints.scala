package endpoints4s.fetch.future

import endpoints4s.fetch

import scala.concurrent.Future

trait Endpoints extends fetch.Endpoints with EndpointsWithCustomErrors

trait EndpointsWithCustomErrors extends fetch.EndpointsWithCustomErrors {

  case class Result[A](value: Future[A], abort: Unit => Unit)

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = new Endpoint[A, B](request, response) {
    def apply(a: A) = {
      val (value, abort) = performFetch(this.request, this.response, a)
      Result(value.toFuture, abort)
    }
  }
}
