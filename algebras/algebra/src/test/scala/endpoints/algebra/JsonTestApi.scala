package endpoints.algebra

import endpoints.algebra

trait JsonTestApi extends algebra.Endpoints with algebra.JsonEntities {

  implicit def userCodec: JsonRequest[User]
  implicit def addresCodec: JsonResponse[Address]


  val smokeEndpoint = endpoint(
    post(path / "user", jsonRequest[User]()),
    jsonResponse[Address]()
  )

}

case class User(name: String, age: Int)

case class Address(street: String, city: String)
