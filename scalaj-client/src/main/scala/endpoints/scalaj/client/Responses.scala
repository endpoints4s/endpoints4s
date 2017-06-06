package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

import scala.util.Try

trait Responses extends algebra.Responses {

  override type Response[A] = HttpResponse[String] => Either[Throwable, A]


  override def emptyResponse: Response[Unit] = {
    case x if x.isError => Try(x.throwError).toEither.map(_ => ())
  }

  override def stringResponse: Response[String] = x => {
    Try(x.throwServerError.body).toEither
  }

}
