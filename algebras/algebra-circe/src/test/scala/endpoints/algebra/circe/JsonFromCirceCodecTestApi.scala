package endpoints.algebra.circe

import endpoints.algebra.{Address, JsonFromCodecTestApi, User}

trait JsonFromCirceCodecTestApi
    extends JsonFromCodecTestApi
    with endpoints.algebra.circe.JsonEntitiesFromCodecs {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit lazy val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit lazy val userEncoder: Encoder[User] = deriveEncoder[User]
  implicit lazy val addressDecoder: Decoder[Address] = deriveDecoder[Address]
  implicit lazy val addressEncoder: Encoder[Address] = deriveEncoder[Address]

  def userCodec: JsonCodec[User] = implicitly
  def addressCodec: JsonCodec[Address] = implicitly

}
