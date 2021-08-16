package endpoints4s.openapi

import endpoints4s.algebra
import endpoints4s.openapi.model.OpenApi
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DefaultsTest extends AnyWordSpec with Matchers {

  "Schemas" should {
    "Document default values for fields" in new Fixtures {
      val expected =
        ujson.Obj(
          "type" -> ujson.Str("object"),
          "properties" -> ujson.Obj(
            "name" -> ujson.Obj(
              "type" -> ujson.Str("string"),
              "description" -> ujson.Str("Name of the user")
            ),
            "age" -> ujson.Obj(
              "type" -> ujson.Str("integer"),
              "format" -> ujson.Str("int32"),
              "default" -> ujson.Num(42)
            )
          ),
          "required" -> ujson.Arr(ujson.Str("name"))
        )
      assert(OpenApi.schemaJson(toSchema(User.schemaWithDefault.docs)) == expected)
    }
  }

  trait Fixtures extends algebra.JsonSchemasFixtures with JsonSchemas

}
