package endpoints.scalaj.client

import endpoints.algebra

import scalaj.http.HttpResponse

trait OptionalResponses extends algebra.OptionalResponses { this: Endpoints =>

  override def option[A](inner: HttpResponse[String] => Either[Throwable, A]): HttpResponse[String] => Either[Throwable, Option[A]] = {
    {
      case resp if resp.code == 404 => Right(None)
      case resp => inner(resp).right.map(Some(_))
    }
  }
}
