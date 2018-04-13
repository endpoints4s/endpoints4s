package endpoints.algebra.circe

import endpoints.algebra
import endpoints.algebra.{Address, JsonFromCodecTestApi, User}

trait JsonFromCirceCodecTestApi
  extends JsonFromCodecTestApi
    with endpoints.algebra.circe.JsonEntitiesFromCodec {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
  implicit val addressDecoder: Decoder[Address] = deriveDecoder[Address]
  implicit val addressEncoder: Encoder[Address] = deriveEncoder[Address]

//  def userCodec = implicitly[JsonCodec[User]]
//  def addressCodec = implicitly[JsonCodec[Address]]

}
