package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait Responses extends algebra.Responses with StatusCodes {

  type Response[A] = HttpResponse[String] => ResponseEntity[A]

  type ResponseEntity[A] = String => Either[Throwable, A]


  def emptyResponse: ResponseEntity[Unit] =
    _ => Right(())

  def textResponse: ResponseEntity[String] =
    s => Right(s)

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    response =>
      if (response.code == statusCode) entity
      else _ => Left(new Throwable(s"Unexpected status code: ${response.code}"))

  def wheneverFound[A](inner: Response[A], notFoundDocs: Documentation): Response[Option[A]] = {
    case resp if resp.code == NotFound => _ => Right(None)
    case resp => entity => inner(resp)(entity).right.map(Some(_))
  }

}
