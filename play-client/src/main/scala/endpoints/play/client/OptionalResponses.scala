package endpoints.play.client

import endpoints.algebra

trait OptionalResponses
  extends algebra.OptionalResponses { this: Endpoints =>

  def option[A](response: Response[A]): Response[Option[A]] =
    wsResponse =>
      if (wsResponse.status == 404) Right(None)
      else response(wsResponse).right.map(Some(_))

}
