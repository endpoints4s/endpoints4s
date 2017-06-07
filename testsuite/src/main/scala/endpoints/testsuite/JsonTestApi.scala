package endpoints.testsuite

import endpoints.algebra
import io.circe.generic.JsonCodec

trait JsonTestApi extends algebra.Endpoints with algebra.JsonEntities {

  implicit def userCodec: JsonRequest[User]
  implicit def addresCodec: JsonResponse[Address]


  val smokeEndpoint = endpoint(
    post(path / "user", jsonRequest[User]),
    jsonResponse[Address]
  )

}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class Address(street: String, city: String)
