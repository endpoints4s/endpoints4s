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
        ("Quux", DocumentedRecord(Field("b", DocumentedJsonSchemas.byteJsonSchema, isOptional = false, documentation = None) :: Nil)) ::
        Nil
      )
    assert(DocumentedJsonSchemas.Foo.schema == expectedSchema)
  }

  "enum" in {
    val expectedSchema =
      DocumentedEnum(DocumentedJsonSchemas.stringJsonSchema, "Red" :: "Blue" :: Nil, Some("Color"))
    assert(DocumentedJsonSchemas.Enum.colorSchema == expectedSchema)
  }

  "recursive" in {
    DocumentedJsonSchemas.recursiveSchema match {
      case DocumentedRecord(List(Field("next", tpe, true, None)), None, None) => assert(tpe.isInstanceOf[LazySchema])
      case _ => fail(s"Unexpected type for 'recSchema': ${DocumentedJsonSchemas.recursiveSchema}")
    }
  }

  "maps" in {
    val expected = DocumentedRecord(Nil, Some(DocumentedJsonSchemas.intJsonSchema), None)
    assert(DocumentedJsonSchemas.intDictionary == expected)
  }

  "tuple" in {
    val expected = Array(Right(DocumentedJsonSchemas.booleanJsonSchema :: DocumentedJsonSchemas.intJsonSchema :: DocumentedJsonSchemas.stringJsonSchema :: Nil))
    assert(DocumentedJsonSchemas.boolIntString == expected)
  }

}
