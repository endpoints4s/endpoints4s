package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait Responses extends algebra.Responses with StatusCodes {

  type Response[A] = HttpResponse[String] => Either[Throwable, A]


  def emptyResponse(docs: Documentation): Response[Unit] = {
    case response if response.code >= OK && response.code < 300 => Right(())
    case response => Left(new Throwable(s"Unexpected status code: ${response.code}"))
  }

  def textResponse(docs: Documentation): Response[String] = x => if (x.code == OK) Right(x.body) else Left(new Throwable(s"Unexpected status code: ${x.code}"))

  def wheneverFound[A](inner: HttpResponse[String] => Either[Throwable, A], notFoundDocs: Documentation): HttpResponse[String] => Either[Throwable, Option[A]] = {
    {
      case resp if resp.code == NotFound => Right(None)
      case resp => inner(resp).right.map(Some(_))
    }
  }

}
