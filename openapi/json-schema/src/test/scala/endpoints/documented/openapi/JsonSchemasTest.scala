package endpoints.documented
package openapi

import org.scalatest.FreeSpec

class JsonSchemasTest extends FreeSpec {

  object DocumentedJsonSchemas
    extends algebra.JsonSchemasTest
      with JsonSchemas

  import DocumentedJsonSchemas.DocumentedJsonSchema._

  "case class" in {
    val expectedSchema =
      DocumentedRecord(
        Field("name", DocumentedJsonSchemas.stringJsonSchema, isOptional = false) ::
        Field("age", DocumentedJsonSchemas.intJsonSchema, isOptional = false) ::
        Nil
      )
    assert(DocumentedJsonSchemas.User.schema == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema =
      DocumentedCoProd(
        ("Bar", DocumentedRecord(Field("s", DocumentedJsonSchemas.stringJsonSchema, isOptional = false) :: Nil)) ::
        ("Baz", DocumentedRecord(Field("i", DocumentedJsonSchemas.intJsonSchema, isOptional = false) :: Nil)) ::
        Nil
      )
    assert(DocumentedJsonSchemas.Foo.schema == expectedSchema)
  }

}
