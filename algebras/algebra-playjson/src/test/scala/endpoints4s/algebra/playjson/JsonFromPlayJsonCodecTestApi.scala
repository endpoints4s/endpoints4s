package endpoints4s.algebra.playjson

import endpoints4s.algebra
import endpoints4s.algebra.{Address, JsonFromCodecTestApi, User}
import play.api.libs.json.{Format, Json}

trait JsonFromPlayJsonCodecTestApi
    extends JsonFromCodecTestApi
    with algebra.playjson.JsonEntitiesFromCodecs {

  implicit lazy val addressCodec: Format[Address] = Json.format[Address]
  implicit lazy val userCodec: Format[User] = Json.format[User]

}
