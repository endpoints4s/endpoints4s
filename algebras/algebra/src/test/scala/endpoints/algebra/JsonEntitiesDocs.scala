package endpoints.algebra

trait JsonEntitiesDocs extends JsonEntities {

  case class User(id: Long, name: String)
  case class CreateUser(name: String)

  implicit def createUserJsonRequest: JsonRequest[CreateUser]
  implicit def userJsonResponse: JsonResponse[User]

  //#json-entities
  endpoint(
    post(path / "user", jsonRequest[CreateUser]()),
    jsonResponse[User]()
  )
  //#json-entities

}
