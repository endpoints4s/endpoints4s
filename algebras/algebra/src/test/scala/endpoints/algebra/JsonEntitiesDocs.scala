package endpoints.algebra

trait JsonEntitiesDocs extends JsonEntities {

  case class User(id: Long, name: String)
  case class CreateUser(name: String)
  trait Error
  sealed trait Validated[+A] extends Product with Serializable {
    def toEither: Either[Seq[Error], A] = this match {
      case Invalid(errors) => Left(errors)
      case Valid(a)        => Right(a)
    }
  }
  case class Valid[+A](a: A) extends Validated[A]
  case class Invalid(errors: Seq[Error]) extends Validated[Nothing]
  object Validated {
    def fromEither[A](either: Either[Seq[Error], A]): Validated[A] = either match {
      case Left(errors) => Invalid(errors)
      case Right(a)     => Valid(a)
    }
  }

  implicit def createUserJsonRequest: JsonRequest[CreateUser]
  implicit def userJsonResponse: JsonResponse[User]
  implicit def errorsJsonResponse: JsonResponse[Seq[Error]]

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
