package endpoints4s.openapi

import endpoints4s.algebra
import endpoints4s.openapi.model.OpenApi
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OneOfTest extends AnyWordSpec with Matchers {

  "Schemas" should {
    "Document enumerated oneOf schemas" in new Fixtures {
      val expected =
        ujson.Obj(
          "oneOf" -> ujson.Arr(
            ujson.Obj(
              "type" -> ujson.Str("integer"),
              "format" -> ujson.Str("int32")
            ),
            ujson.Obj("type" -> ujson.Str("boolean"))
          )
        )
      assert(OpenApi.schemaJson(toSchema(intOrBoolean.docs)) == expected)
    }
  }

  trait Fixtures extends algebra.JsonSchemasFixtures with JsonSchemas

}
