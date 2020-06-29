package endpoints4s.algebra

trait SumTypedEntitiesTestApi extends Endpoints with JsonEntitiesFromCodecs {

  implicit def userCodec: JsonCodec[User]

  def sumTypedEndpoint =
    endpoint[Either[User, String], Either[User, String]](
      post(path / "user-or-name", jsonRequest[User].orElse(textRequest)),
      ok(jsonResponse[User]).orElse(ok(textResponse))
    )

  def sumTypedEndpoint2 =
    endpoint[Either[User, String], Unit](
      post(path / "user-or-name", jsonRequest[User].orElse(textRequest)),
      ok(emptyResponse)
    )
}
