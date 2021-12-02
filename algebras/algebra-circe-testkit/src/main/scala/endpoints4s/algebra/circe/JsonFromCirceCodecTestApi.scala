package endpoints4s.algebra.circe

import endpoints4s.algebra.{Address, JsonFromCodecTestApi, User}

trait JsonFromCirceCodecTestApi
    extends JsonFromCodecTestApi
    with endpoints4s.algebra.circe.JsonEntitiesFromCodecs {

  import io.circe._

  implicit lazy val userDecoder: Decoder[User] =
    Decoder
      .instance(_.get[String]("name"))
      .product(Decoder.instance(_.get[Int]("age")))
      .map { case (name, age) => User(name, age) }
  implicit lazy val userEncoder: Encoder[User] =
    Encoder.instance { user =>
      Json.obj(
        "name" -> Json.fromString(user.name),
        "age" -> Json.fromInt(user.age)
      )
    }
  implicit lazy val addressDecoder: Decoder[Address] =
    Decoder
      .instance(_.get[String]("street"))
      .product(Decoder.instance(_.get[String]("city")))
      .map { case (street, city) => Address(street, city) }
  implicit lazy val addressEncoder: Encoder[Address] =
    Encoder.instance { address =>
      Json.obj(
        "street" -> Json.fromString(address.street),
        "city" -> Json.fromString(address.city)
      )
    }

  def userCodec: JsonCodec[User] = implicitly
  def addressCodec: JsonCodec[Address] = implicitly

}
