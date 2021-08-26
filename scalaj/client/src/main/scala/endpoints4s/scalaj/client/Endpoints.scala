package endpoints4s.scalaj.client

import endpoints4s.algebra

import scala.concurrent.{ExecutionContext, Future}

/** @group interpreters
  */
trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

/** @group interpreters
  */
trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Requests
    with Responses {

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    Endpoint(request, response)

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

  case class Endpoint[Req, Resp](
      request: Request[Req],
      response: Response[Resp]
  ) {

    /** This method just wraps a call in a Future and is not real async call
      */
    def callAsync(args: Req)(implicit ec: ExecutionContext): Future[Resp] =
      Future {
        concurrent.blocking {
          callUnsafe(args)
        }
      }

    def callUnsafe(args: Req): Resp =
      call(args) match {
        case Left(ex) => throw ex
        case Right(x) => x
      }

    def call(args: Req): Either[Throwable, Resp] = {
      def mapPartialResponseEntity[A](
          entity: ResponseEntity[A]
      )(f: A => Either[Throwable, Resp]): ResponseEntity[Resp] =
        httpEntity => entity(httpEntity).flatMap(f)

      val resp = request(args).asString
      val maybeResponse = response(resp)
      def maybeClientErrors =
        clientErrorsResponse(resp)
          .map(
            mapPartialResponseEntity(_)(clientErrors =>
              Left(
                new Exception(
                  clientErrorsToInvalid(clientErrors).errors.mkString(". ")
                )
              )
            )
          )
      def maybeServerError =
        serverErrorResponse(resp)
          .map(
            mapPartialResponseEntity(_)(serverError => Left(serverErrorToThrowable(serverError)))
          )
      maybeResponse
        .orElse(maybeClientErrors)
        .orElse(maybeServerError)
        .toRight(new Throwable(s"Unexpected response status: ${resp.code}"))
        .flatMap(_(resp.body))
    }
  }

}
