package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

import scala.util.{Failure, Success, Try}

trait Responses extends algebra.Responses {

   type Response[A] = HttpResponse[String] => Either[Throwable, A]


   def emptyResponse: Response[Unit] = {
    case x if x.isError => Try(x.throwError) match {
      case Failure(ex) => Left(ex)
      case Success(_) => Right(())
    }
  }

   def textResponse: Response[String] = x => if(x.code == 200) Right(x.body) else Left(new Throwable(s"Unexpected status code: ${x.code}"))

}
