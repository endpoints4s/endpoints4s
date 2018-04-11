package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

trait Responses extends algebra.Responses {

   type Response[A] = HttpResponse[String] => Either[Throwable, A]


   def emptyResponse: Response[Unit] = {
    case response if response.code >= 200 && response.code <300 => Right(())
    case response => Left(new Throwable(s"Unexpected status code: ${response.code}"))
  }

   def textResponse: Response[String] = x => if(x.code == 200) Right(x.body) else Left(new Throwable(s"Unexpected status code: ${x.code}"))

}
