package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.{InvariantFunctor, algebra}
import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait Responses extends algebra.Responses with StatusCodes { this: algebra.Errors =>

  type Response[A] = HttpResponse[String] => Option[ResponseEntity[A]]

  implicit lazy val responseInvFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        resp => fa(resp).map(entity => s => entity(s).right.map(f))
    }

  type ResponseEntity[A] = String => Either[Throwable, A]

  def emptyResponse: ResponseEntity[Unit] =
    _ => Right(())

  def textResponse: ResponseEntity[String] =
    s => Right(s)

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    response =>
      if (response.code == statusCode) Some(entity)
      else None

  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]] =
    resp =>
      responseA(resp).map(entity => (s: String) => entity(s).right.map(Left(_)))
        .orElse(responseB(resp).map(entity => (s: String) => entity(s).right.map(Right(_))))

}
