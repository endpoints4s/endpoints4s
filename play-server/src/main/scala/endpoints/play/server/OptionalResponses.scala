package endpoints.play.server

import endpoints.algebra
import play.api.mvc.Results

trait OptionalResponses extends algebra.OptionalResponses with Endpoints {

  /**
    * A response encoder that maps `None` to an empty HTTP result with status 404
    */
  def option[A](response: Response[A]): Response[Option[A]] = {
    case Some(a) => response(a)
    case None => Results.NotFound
  }

}
