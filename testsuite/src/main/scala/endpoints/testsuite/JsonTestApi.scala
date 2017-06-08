package endpoints.testsuite

import endpoints.algebra
import endpoints.algebra.JsonBuilders
import io.circe.generic.JsonCodec

trait JsonTestApi extends algebra.Endpoints with algebra.JsonEntities with JsonBuilders {

  implicit def userCodec: JsonRequest[User]

  implicit def addresCodec: JsonResponse[Address]


  val jsonEndpoint = endpoint(
    post(path / "user", jsonRequest[User]),
    jsonResponse[Address]
  )

  val jsonEndpointViaBuilder =
    anEndpoint
      .withMethod(Post)
      .withUrl(path / "user")
      .withJsonRequest[User]
      .withJsonResponse[Address]
      .build

}

@JsonCodec
case class User(name: String, age: Int)

@JsonCodec
case class Address(street: String, city: String)
