package endpoints4s.playjson

import endpoints4s.{Invalid, Valid, Validated, algebra}
import play.api.libs.json.{
  JsArray,
  JsBoolean,
  JsError,
  JsNull,
  JsNumber,
  JsObject,
  JsString,
  JsSuccess,
  JsValue
}
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasOptionalFieldsTest
    extends AnyFreeSpec
    with algebra.JsonSchemasOptionalFieldsTest
    with JsonSchemas {

  object Json extends Json {
    type Json = JsValue
    def obj(fields: (String, Json)*): Json = JsObject(fields)
    def arr(items: Json*): Json = JsArray(items)
    def num(x: BigDecimal): Json = JsNumber(x)
    def str(s: String): Json = JsString(s)
    def bool(b: Boolean): Json = JsBoolean(b)
    def `null`: Json = JsNull
  }

  def decodeJson[A](schema: JsonSchema[A], json: Json.Json): Validated[A] =
    schema.reads.reads(json) match {
      case JsSuccess(a, _) => Valid(a)
      case JsError(errors) =>
        val stringErrors =
          for {
            (_, pathErrors) <- errors
            error <- pathErrors
            message <- error.messages
          } yield message
        Invalid(stringErrors.toList)
    }

  def encodeJson[A](schema: JsonSchema[A], value: A): Json.Json =
    schema.writes.writes(value)

}
