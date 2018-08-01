package endpoints.algebra

trait JsonTestApi {

  val entities: JsonEntities

  import entities._
  import endpoints._
  import requests._
  import urls._

  implicit def userCodec: JsonRequest[User]
  implicit def addresCodec: JsonResponse[Address]


  val smokeEndpoint = endpoint(
    post(path / "user", jsonRequest[User]()),
    jsonResponse[Address]()
  )

}

case class User(name: String, age: Int)

case class Address(street: String, city: String)
