package endpoints.scalaj.client

import scalaj.http.HttpResponse
import endpoints.algebra

trait Responses extends algebra.Responses {

  override type Response[A] = HttpResponse[String] => A


  override def emptyResponse: Response[Unit] = x => {
    x.throwServerError
    ()
  }

  override def stringResponse: Response[String] = x => {
    x.throwServerError.body
  }

}
