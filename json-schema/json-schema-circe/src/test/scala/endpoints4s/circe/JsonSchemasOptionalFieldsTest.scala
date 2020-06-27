package endpoints4s
package circe

import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasOptionalFieldsTest
    extends AnyFreeSpec
    with algebra.JsonSchemasOptionalFieldsTest
    with JsonSchemas {

  object Json extends Json {
    type Json = io.circe.Json
    def obj(fields: (String, Json)*): Json = io.circe.Json.obj(fields: _*)
    def arr(items: Json*): Json = io.circe.Json.arr(items: _*)
    def num(x: BigDecimal): Json = io.circe.Json.fromBigDecimal(x)
    def str(s: String): Json = io.circe.Json.fromString(s)
    def bool(b: Boolean): Json = io.circe.Json.fromBoolean(b)
    def `null`: Json = io.circe.Json.Null
  }

  def decodeJson[A](schema: JsonSchema[A], json: Json.Json): Validated[A] =
    schema.decoder.decodeAccumulating(json.hcursor) match {
      case cats.data.Validated.Valid(a)   => Valid(a)
      case cats.data.Validated.Invalid(e) => Invalid(e.toList.map(_.message))
    }

  def encodeJson[A](schema: JsonSchema[A], value: A): Json.Json =
    schema.encoder(value)

}
