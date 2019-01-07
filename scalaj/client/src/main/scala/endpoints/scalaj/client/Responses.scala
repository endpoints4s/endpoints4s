package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait Responses extends algebra.Responses {

  type Response[A] = HttpResponse[String] => Either[Throwable, A]


  def emptyResponse(docs: Documentation): Response[Unit] = {
    case response if response.code >= 200 && response.code < 300 => Right(())
    case response => Left(new Throwable(s"Unexpected status code: ${response.code}"))
  }

  def textResponse(docs: Documentation): Response[String] = x => if (x.code == 200) Right(x.body) else Left(new Throwable(s"Unexpected status code: ${x.code}"))

  def wheneverFound[A](inner: HttpResponse[String] => Either[Throwable, A], notFoundDocs: Documentation): HttpResponse[String] => Either[Throwable, Option[A]] = {
    {
      case resp if resp.code == 404 => Right(None)
      case resp => inner(resp).right.map(Some(_))
    }
  }

}
