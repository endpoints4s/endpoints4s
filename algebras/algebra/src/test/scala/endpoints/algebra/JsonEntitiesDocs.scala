package endpoints.algebra

import endpoints.Validated

trait JsonEntitiesDocs extends JsonEntities {

  type Error = String

  case class User(id: Long, name: String)
  case class CreateUser(name: String)

  implicit def createUserJsonRequest: JsonRequest[CreateUser]
  implicit def userJsonResponse: JsonResponse[User]
  implicit def errorsJsonResponse: JsonResponse[Seq[String]]

  //#json-entities
  endpoint(
    post(path / "user", jsonRequest[CreateUser]),
    ok(jsonResponse[User])
  )
  //#json-entities

  //#response-or-not-found
  val getUser: Endpoint[Long, Option[User]] =
    endpoint(
      get(path / "user" / segment[Long]("id")),
      ok(jsonResponse[User]).orNotFound()
    )
  //#response-or-not-found

  //#response-or-else
  val validUserResponse: Response[Either[Seq[Error], User]] =
    badRequest(jsonResponse[Seq[Error]]).orElse(ok(jsonResponse[User]))
  //#response-or-else

  locally {
    //#response-xmap
    val validUserResponse: Response[Validated[User]] =
      badRequest(jsonResponse[Seq[Error]]).orElse(ok(jsonResponse[User]))
        .xmap(Validated.fromEither)(_.toEither)
    //#response-xmap
  }

}
