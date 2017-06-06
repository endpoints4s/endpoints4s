package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

import scala.util.{Failure, Success, Try}

trait Responses extends algebra.Responses {

  override type Response[A] = HttpResponse[String] => Either[Throwable, A]


  override def emptyResponse: Response[Unit] = {
    case x if x.isError => Try(x.throwError) match {
      case Failure(ex) => Left(ex)
      case Success(_) => Right(())
    }
  }

  override def stringResponse: Response[String] = x => {
    Try(x.throwError.body) match {
      case Failure(ex) => Left(ex)
      case Success(x) => Right(x)
    }
  }

}
