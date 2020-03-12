package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.{
  Invalid,
  InvariantFunctor,
  PartialInvariantFunctor,
  Semigroupal,
  Tupler,
  Valid,
  Validated,
  algebra
}
import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait Responses extends algebra.Responses with StatusCodes {
  this: algebra.Errors =>

  type Response[A] = HttpResponse[String] => Option[ResponseEntity[A]]

  implicit lazy val responseInvFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        resp => fa(resp).map(entity => s => entity(s).map(f))
    }

  type ResponseEntity[A] = String => Either[Throwable, A]

  def emptyResponse: ResponseEntity[Unit] =
    _ => Right(())

  def textResponse: ResponseEntity[String] =
    s => Right(s)

  type ResponseHeaders[A] = Map[String, Seq[String]] => Validated[A]

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(
          implicit tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  implicit def responseHeadersInvFunctor
      : PartialInvariantFunctor[ResponseHeaders] =
    new PartialInvariantFunctor[ResponseHeaders] {
      def xmapPartial[A, B](
          fa: ResponseHeaders[A],
          f: A => Validated[B],
          g: B => A
      ): ResponseHeaders[B] =
        headers => fa(headers).flatMap(f)
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] = _ => Valid(())

  def responseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[String] =
    headers =>
      Validated.fromOption(
        headers.get(name.toLowerCase).map(_.mkString(", "))
      )(s"Missing response header '$name'")

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] =
    headers => Valid(headers.get(name.toLowerCase).map(_.mkString(", ")))

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B] = emptyResponseHeaders
  )(
      implicit
      tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    response =>
      if (response.code == statusCode) {
        headers(response.headers) match {
          case Valid(b) => Some(s => entity(s).map(tupler(_, b)))
          case Invalid(errors) =>
            Some(_ => Left(new Exception(errors.mkString(". "))))
        }
      } else None

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] =
    resp =>
      responseA(resp)
        .map(entity => (s: String) => entity(s).map(Left(_)))
        .orElse(
          responseB(resp).map(entity => (s: String) => entity(s).map(Right(_)))
        )

}
