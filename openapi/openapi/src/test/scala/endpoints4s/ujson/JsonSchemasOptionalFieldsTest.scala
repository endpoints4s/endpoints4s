package endpoints4s.ujson

import endpoints4s.{Validated, algebra}
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasOptionalFieldsTest
    extends AnyFreeSpec
    with algebra.JsonSchemasOptionalFieldsTest
    with JsonSchemas {

  override def encodersSkipDefaultValues: Boolean = false

  object Json extends Json {
    type Json = ujson.Value
    def obj(fields: (String, Json)*): Json = ujson.Obj.from(fields)
    def arr(items: Json*): Json = ujson.Arr.from(items)
    def num(x: BigDecimal): Json = ujson.Num(x.doubleValue)
    def str(s: String): Json = ujson.Str(s)
    def bool(b: Boolean): Json = ujson.Bool(b)
    def `null`: Json = ujson.Null
  }

  def decodeJson[A](schema: JsonSchema[A], json: Json.Json): Validated[A] =
    schema.decoder.decode(json)

  def encodeJson[A](schema: JsonSchema[A], value: A): Json.Json =
    schema.encoder.encode(value)

}

class JsonSchemasOptionalFieldsNoValueTest
    extends AnyFreeSpec
    with algebra.JsonSchemasOptionalFieldsTest
    with JsonSchemas {

  override def encodersSkipDefaultValues: Boolean = true

  object Json extends Json {
    type Json = ujson.Value
    def obj(fields: (String, Json)*): Json = ujson.Obj.from(fields)
    def arr(items: Json*): Json = ujson.Arr.from(items)
    def num(x: BigDecimal): Json = ujson.Num(x.doubleValue)
    def str(s: String): Json = ujson.Str(s)
    def bool(b: Boolean): Json = ujson.Bool(b)
    def `null`: Json = ujson.Null
  }

  def decodeJson[A](schema: JsonSchema[A], json: Json.Json): Validated[A] =
    schema.decoder.decode(json)

  def encodeJson[A](schema: JsonSchema[A], value: A): Json.Json =
    schema.encoder.encode(value)

}
