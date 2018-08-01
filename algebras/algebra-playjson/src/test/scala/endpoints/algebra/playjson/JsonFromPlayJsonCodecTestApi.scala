package endpoints.algebra.playjson

import endpoints.algebra.{Address, JsonFromCodecTestApi, User}
import play.api.libs.json.{Format, Json}


trait JsonFromPlayJsonCodecTestApi
  extends JsonFromCodecTestApi {

  override val entities: JsonEntitiesFromCodec

  implicit lazy val addressCodec: Format[Address] = Json.format[Address]
  implicit lazy val userCodec: Format[User] = Json.format[User]

}
