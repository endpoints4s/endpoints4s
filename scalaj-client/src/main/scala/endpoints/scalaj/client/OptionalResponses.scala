package endpoints.scalaj.client

import endpoints.algebra

trait OptionalResponses extends algebra.OptionalResponses { self: Endpoints =>

  def option[A](response: Response[A]): Response[Option[A]] =
    resp => {
      if(resp.code == 404)
        Right(None)
      else
        response(resp).right.map(Some(_))
    }

}