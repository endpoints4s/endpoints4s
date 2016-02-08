package example

import julienrf.endpoints.Endpoints

trait ApiAlg extends Endpoints {

  implicit def userOutput: ResponseMarshaller[User]

  val index = endpoint(get(chained(static("user"), dynamic)), jsonEntity[User])

}

case class User(name: String, age: Int)

object User {
  import io.circe.generic.semiauto.deriveFor
  import io.circe.{Decoder, Encoder}
  implicit val enc: Encoder[User] = deriveFor[User].encoder
  implicit val dec: Decoder[User] = deriveFor[User].decoder
}