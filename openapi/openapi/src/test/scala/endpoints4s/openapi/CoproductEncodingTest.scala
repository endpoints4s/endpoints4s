package endpoints4s.openapi

import endpoints4s.algebra
import endpoints4s.openapi.model._
import org.scalatest.freespec.AnyFreeSpec

class CoproductEncodingTest extends AnyFreeSpec {

  trait CoproductEncodingAlg
      extends algebra.Endpoints
      with algebra.JsonEntitiesFromSchemas
      with algebra.JsonSchemasFixtures {

    implicit val fooSchema = Foo.schema.named("Foo")

    val foo = endpoint(get(path / "foo"), ok(jsonResponse[Foo]))
  }

  object OneOfStrategyDocs
      extends CoproductEncodingAlg
      with Endpoints
      with JsonEntitiesFromSchemas {

    override def coproductEncoding = CoproductEncoding.OneOf
    val api = openApi(Info("OneOf", "1"))(foo)
  }

  object OneOfWithBaseRefStrategyDocs
      extends CoproductEncodingAlg
      with Endpoints
      with JsonEntitiesFromSchemas {

    override def coproductEncoding = CoproductEncoding.OneOfWithBaseRef
    val api = openApi(Info("OneOf", "1"))(foo)
  }

  "OneOf schema encoding" in {
    val expectedJsonSchema = ujson.Obj(
      "discriminator" -> ujson.Obj(
        "propertyName" -> ujson.Str("type")
      ),
      "oneOf" -> ujson.Arr(
        ujson.Obj(
          "type" -> ujson.Str("object"),
          "properties" -> ujson.Obj(
            "type" -> ujson.Obj(
              "type" -> ujson.Str("string"),
              "enum" -> ujson.Arr(ujson.Str("Bar")),
              "example" -> ujson.Str("Bar")
            ),
            "s" -> ujson.Obj(
              "type" -> ujson.Str("string")
            )
          ),
          "required" -> ujson.Arr(ujson.Str("type"), ujson.Str("s"))
        ),
        ujson.Obj(
          "type" -> ujson.Str("object"),
          "properties" -> ujson.Obj(
            "type" -> ujson.Obj(
              "type" -> ujson.Str("string"),
              "enum" -> ujson.Arr(ujson.Str("Baz")),
              "example" -> ujson.Str("Baz")
            ),
            "i" -> ujson.Obj(
              "type" -> ujson.Str("integer"),
              "format" -> ujson.Str("int32")
            )
          ),
          "required" -> ujson.Arr(ujson.Str("type"), ujson.Str("i"))
        ),
        ujson.Obj(
          "type" -> ujson.Str("object"),
          "properties" -> ujson.Obj(
            "type" -> ujson.Obj(
              "type" -> ujson.Str("string"),
              "enum" -> ujson.Arr(ujson.Str("Bax")),
              "example" -> ujson.Str("Bax")
            )
          ),
          "required" -> ujson.Arr(ujson.Str("type"))
        ),
        ujson.Obj(
          "type" -> ujson.Str("object"),
          "properties" -> ujson.Obj(
            "type" -> ujson.Obj(
              "type" -> ujson.Str("string"),
              "enum" -> ujson.Arr(ujson.Str("Qux")),
              "example" -> ujson.Str("Qux")
            )
          ),
          "required" -> ujson.Arr(ujson.Str("type"))
        ),
        ujson.Obj(
          "type" -> ujson.Str("object"),
          "properties" -> ujson.Obj(
            "type" -> ujson.Obj(
              "type" -> ujson.Str("string"),
              "enum" -> ujson.Arr(ujson.Str("Quux")),
              "example" -> ujson.Str("Quux")
            ),
            "b" -> ujson.Obj(
              "type" -> ujson.Str("integer")
            )
          ),
          "required" -> ujson.Arr(ujson.Str("type"), ujson.Str("b"))
        )
      )
    )
    assert(
      OpenApi.schemaJson(OneOfStrategyDocs.api.components.schemas("Foo")) == expectedJsonSchema
    )
  }

  "OneOfWithBaseRef schema encoding" in {
    val expectedJsonSchema = ujson.Obj(
      "discriminator" -> ujson.Obj(
        "propertyName" -> ujson.Str("type")
      ),
      "oneOf" -> ujson.Arr(
        ujson.Obj(
          "allOf" -> ujson.Arr(
            ujson.Obj("$ref" -> ujson.Str("#/components/schemas/Foo")),
            ujson.Obj(
              "type" -> ujson.Str("object"),
              "properties" -> ujson.Obj(
                "type" -> ujson.Obj(
                  "type" -> ujson.Str("string"),
                  "enum" -> ujson.Arr(ujson.Str("Bar")),
                  "example" -> ujson.Str("Bar")
                ),
                "s" -> ujson.Obj(
                  "type" -> ujson.Str("string")
                )
              ),
              "required" -> ujson.Arr(ujson.Str("type"), ujson.Str("s"))
            )
          )
        ),
        ujson.Obj(
          "allOf" -> ujson.Arr(
            ujson.Obj("$ref" -> ujson.Str("#/components/schemas/Foo")),
            ujson.Obj(
              "type" -> ujson.Str("object"),
              "properties" -> ujson.Obj(
                "type" -> ujson.Obj(
                  "type" -> ujson.Str("string"),
                  "enum" -> ujson.Arr(ujson.Str("Baz")),
                  "example" -> ujson.Str("Baz")
                ),
                "i" -> ujson.Obj(
                  "type" -> ujson.Str("integer"),
                  "format" -> ujson.Str("int32")
                )
              ),
              "required" -> ujson.Arr(ujson.Str("type"), ujson.Str("i"))
            )
          )
        ),
        ujson.Obj(
          "allOf" -> ujson.Arr(
            ujson.Obj("$ref" -> ujson.Str("#/components/schemas/Foo")),
            ujson.Obj(
              "type" -> ujson.Str("object"),
              "properties" -> ujson.Obj(
                "type" -> ujson.Obj(
                  "type" -> ujson.Str("string"),
                  "enum" -> ujson.Arr(ujson.Str("Bax")),
                  "example" -> ujson.Str("Bax")
                )
              ),
              "required" -> ujson.Arr(ujson.Str("type"))
            )
          )
        ),
        ujson.Obj(
          "allOf" -> ujson.Arr(
            ujson.Obj("$ref" -> ujson.Str("#/components/schemas/Foo")),
            ujson.Obj(
              "type" -> ujson.Str("object"),
              "properties" -> ujson.Obj(
                "type" -> ujson.Obj(
                  "type" -> ujson.Str("string"),
                  "enum" -> ujson.Arr(ujson.Str("Qux")),
                  "example" -> ujson.Str("Qux")
                )
              ),
              "required" -> ujson.Arr(ujson.Str("type"))
            )
          )
        ),
        ujson.Obj(
          "allOf" -> ujson.Arr(
            ujson.Obj("$ref" -> ujson.Str("#/components/schemas/Foo")),
            ujson.Obj(
              "type" -> ujson.Str("object"),
              "properties" -> ujson.Obj(
                "type" -> ujson.Obj(
                  "type" -> ujson.Str("string"),
                  "enum" -> ujson.Arr(ujson.Str("Quux")),
                  "example" -> ujson.Str("Quux")
                ),
                "b" -> ujson.Obj(
                  "type" -> ujson.Str("integer")
                )
              ),
              "required" -> ujson.Arr(ujson.Str("type"), ujson.Str("b"))
            )
          )
        )
      )
    )
    assert(
      OpenApi.schemaJson(
        OneOfWithBaseRefStrategyDocs.api.components.schemas("Foo")
      ) == expectedJsonSchema
    )
  }
}
