package endpoints.testsuite

import endpoints.algebra

/**
  * Created by wpitula on 6/5/17.
  */
trait JsonTestApi extends algebra.Endpoints with algebra.JsonEntities {
  import JsonTestApi._

  implicit def userCodec: JsonRequest[User]
  implicit def addresCodec: JsonResponse[Address]


  val smokeEndpoint = endpoint(
    post(path / "user", jsonRequest[User]),
    jsonResponse[Address]
  )

}

object JsonTestApi {
  case class User(name: String, age: Int)
  case class Address(street: String, city: String)
}
