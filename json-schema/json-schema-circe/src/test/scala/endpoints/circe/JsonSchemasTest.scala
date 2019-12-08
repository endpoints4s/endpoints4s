package endpoints
package circe

import io.circe.{DecodingFailure, Json}
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasTest extends AnyFreeSpec {

  object JsonSchemasCodec
    extends algebra.JsonSchemasTest
      with circe.JsonSchemas

  import JsonSchemasCodec.{User, Foo, Bar}

  "case class" in {
    val userJson =
      Json.obj(
        "name" -> Json.fromString("Julien"),
        "age"  -> Json.fromInt(32)
      )
    val user = User("Julien", 32)

    assert(User.schema.decoder.decodeJson(userJson).right.exists(_ == user))
    assert(User.schema.encoder.apply(user) == userJson)

    assert(User.schema2.decoder.decodeJson(userJson).right.exists(_ == user))
    assert(User.schema2.encoder.apply(user) == userJson)
  }

  "sealed trait" in {
    val barJson =
      Json.obj(
        "type" -> Json.fromString("Bar"),
        "s" -> Json.fromString("foo")
      )
    val bar = Bar("foo")
    assert(Foo.schema.decoder.decodeJson(barJson).right.exists(_ == bar))
    assert(Foo.schema.encoder.apply(bar) == barJson)

    assert(Foo.schema.decoder.decodeJson(Json.obj()).swap.right.exists(_ == DecodingFailure("Missing type discriminator field 'type'!", Nil)))
    val wrongJson = Json.obj("type" -> Json.fromString("Unknown"), "s" -> Json.fromString("foo"))
    assert(Foo.schema.decoder.decodeJson(wrongJson).swap.right.exists(_ == DecodingFailure("No decoder for discriminator 'Unknown'!", Nil)))
  }

  "recursive type" in {
    val json = Json.obj("next" -> Json.obj("next" -> Json.obj()))
    val rec = JsonSchemasCodec.Recursive(Some(JsonSchemasCodec.Recursive(Some(JsonSchemasCodec.Recursive(None)))))
    assert(JsonSchemasCodec.recursiveSchema.decoder.decodeJson(json).right.exists(_ == rec))
    assert(JsonSchemasCodec.recursiveSchema.encoder(rec) == json)
  }

  "tuple" in {
    val json = Json.arr(Json.True, Json.fromInt(42), Json.fromString("foo"))
    val value = (true, 42, "foo")
    assert(JsonSchemasCodec.boolIntString.decoder.decodeJson(json).right.exists(_ == value))
    assert(JsonSchemasCodec.boolIntString.encoder(value) == json)
  }

  "refined JsonSchema" in {
    val validJson = Json.fromInt(42)
    val validValue = 42
    assert(JsonSchemasCodec.evenNumberSchema.encoder(validValue) == validJson)
    assert(JsonSchemasCodec.evenNumberSchema.decoder.decodeJson(validJson).right.exists(_ == validValue))

    val invalidJson = Json.fromInt(41)
    val invalidValue = 41
    assert(JsonSchemasCodec.evenNumberSchema.encoder(invalidValue) == invalidJson)
    assert(JsonSchemasCodec.evenNumberSchema.decoder.decodeJson(invalidJson).left.exists(_ == DecodingFailure("Invalid even integer '41'", Nil)))
  }

  "refined Tagged" in {
    val validJson =
      Json.obj(
        "type" -> Json.fromString("Baz"),
        "i" -> Json.fromInt(42)
      )
    val validValue = JsonSchemasCodec.RefinedTagged(42)
    assert(JsonSchemasCodec.refinedTaggedSchema.encoder(validValue) == validJson)
    assert(JsonSchemasCodec.refinedTaggedSchema.decoder.decodeJson(validJson).right.exists(_ == validValue))

    val invalidJson =
      Json.obj(
        "type" -> Json.fromString("Bar"),
        "s" -> Json.fromString("foo")
      )
    assert(JsonSchemasCodec.refinedTaggedSchema.decoder.decodeJson(invalidJson).left.exists(_ == DecodingFailure("Invalid tagged alternative", Nil)))
  }

  "enumeration" in {
    import JsonSchemasCodec.NonStringEnum.{Foo, enumSchema}
    val validValue = Foo("bar")
    val validJson = Json.obj("quux" -> Json.fromString("bar"))
    assert(enumSchema.encoder(validValue) == validJson)
    assert(enumSchema.decoder.decodeJson(validJson).right.exists(_ == validValue))
    val invalidJson = Json.obj("quux" -> Json.fromString("wrong"))
    assert(enumSchema.decoder.decodeJson(invalidJson).left.exists(_ == DecodingFailure("Invalid value: {\n  \"quux\" : \"wrong\"\n}. Valid values are: {\n  \"quux\" : \"bar\"\n}, {\n  \"quux\" : \"baz\"\n}.", Nil)))
  }
}
