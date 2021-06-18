package endpoints4s.algebra.playjson

import endpoints4s.algebra
import endpoints4s.algebra.{Address, JsonFromCodecTestApi, User}
import play.api.libs.json.{Format, JsPath}
import play.api.libs.functional.syntax._

trait JsonFromPlayJsonCodecTestApi
    extends JsonFromCodecTestApi
    with algebra.playjson.JsonEntitiesFromCodecs {

  implicit lazy val addressCodec: Format[Address] =
    (
      (JsPath \ "street").format[String] ~
        (JsPath \ "city").format[String]
    )(Address(_, _), (address: Address) => (address.street, address.city))
  implicit lazy val userCodec: Format[User] =
    (
      (JsPath \ "name").format[String] ~
        (JsPath \ "age").format[Int]
    )(User(_, _), (user: User) => (user.name, user.age))

}
