package endpoints.scalaj.client


trait OptionalResponses extends Endpoints {

  def option[A](response: Response[A]): Response[Option[A]] =
    resp => {
      if(resp.code == 404)
        Right(None)
      else
        response(resp).right.map(Some(_))
    }

}