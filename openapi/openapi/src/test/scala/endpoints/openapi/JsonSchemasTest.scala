package endpoints.openapi

import endpoints.algebra
import org.scalatest.FreeSpec

class JsonSchemasTest extends FreeSpec {

  object DocumentedJsonSchemas
    extends algebra.JsonSchemasTest
      with JsonSchemas

  import DocumentedJsonSchemas.DocumentedJsonSchema._

  "case class" in {
    val expectedSchema =
      DocumentedRecord(
        Field("name", DocumentedJsonSchemas.stringJsonSchema, isOptional = false, documentation = Some("Name of the user")) ::
        Field("age", DocumentedJsonSchemas.intJsonSchema, isOptional = false, documentation = None) ::
        Nil
      )
    assert(DocumentedJsonSchemas.User.schema == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema =
      DocumentedCoProd(
        ("Bar", DocumentedRecord(Field("s", DocumentedJsonSchemas.stringJsonSchema, isOptional = false, documentation = None) :: Nil)) ::
        ("Baz", DocumentedRecord(Field("i", DocumentedJsonSchemas.intJsonSchema, isOptional = false, documentation = None) :: Nil)) ::
        ("Bax", DocumentedRecord(Nil)) ::
        ("Qux", DocumentedRecord(Nil)) ::
        Nil
      )
    assert(DocumentedJsonSchemas.Foo.schema == expectedSchema)
  }

  "enum" in {
    val expectedSchema =
      DocumentedEnum(DocumentedJsonSchemas.stringJsonSchema, Seq("Red", "Blue"))
    assert(DocumentedJsonSchemas.Enum.colorSchema == expectedSchema)
  }

}
