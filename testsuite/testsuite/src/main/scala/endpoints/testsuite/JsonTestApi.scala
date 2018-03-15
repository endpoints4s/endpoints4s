package endpoints.testsuite

import endpoints.algebra
import io.circe.generic.JsonCodec
import play.api.libs.json.{Format, Json}

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

object User {
  implicit val playjsonCodec: Format[User] = Json.format[User]
}

@JsonCodec
case class Address(street: String, city: String)

object Address {
  implicit val playjsonCodec: Format[Address] = Json.format[Address]
}
