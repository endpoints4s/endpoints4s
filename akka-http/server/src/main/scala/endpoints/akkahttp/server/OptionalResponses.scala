package endpoints.akkahttp.server

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives
import endpoints.algebra

trait OptionalResponses extends algebra.OptionalResponses with Endpoints {

  /**
    * A response encoder that maps `None` to an empty HTTP result with status 404
    */
  def option[A](response: Response[A]): Response[Option[A]] = {
    case Some(a) => response(a)
    case None => Directives.complete(HttpResponse(StatusCodes.NotFound))
  }

}
