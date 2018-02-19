package endpoints.documented
package circe

import io.circe.{DecodingFailure, Json}
import org.scalatest.FreeSpec

class JsonSchemasTest extends FreeSpec {

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
  }

  "sealed trait" in {
    val barJson =
      Json.obj(
        "Bar" -> Json.obj(
          "s" -> Json.fromString("foo")
        )
      )
    val bar = Bar("foo")
    assert(Foo.schema.decoder.decodeJson(barJson).right.exists(_ == bar))
    assert(Foo.schema.encoder.apply(bar) == barJson)

    assert(Foo.schema.decoder.decodeJson(Json.obj()).swap.right.exists(_ == DecodingFailure("Missing type tag field", Nil)))
    val wrongJson = Json.obj("Unknown" -> Json.obj("s" -> Json.fromString("foo")))
    assert(Foo.schema.decoder.decodeJson(wrongJson).swap.right.exists(_ == DecodingFailure("No decoder for type tag Unknown", Nil)))
  }

}
