package endpoints.openapi

import endpoints.algebra
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasTest extends AnyFreeSpec {

  object DocumentedJsonSchemas
    extends algebra.JsonSchemasTest
      with JsonSchemas

  import DocumentedJsonSchemas.DocumentedJsonSchema._

  "case class" in {
    val expectedSchema =
      StrictDocumentedRecord(
        Field("name", DocumentedJsonSchemas.defaultStringJsonSchema.docs, isOptional = false, documentation = Some("Name of the user")) ::
        Field("age", DocumentedJsonSchemas.intJsonSchema.docs, isOptional = false, documentation = None) ::
        Nil
      )
    assert(DocumentedJsonSchemas.User.schema.docs == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema =
      StrictDocumentedCoProd(
        ("Bar", StrictDocumentedRecord(Field("s", DocumentedJsonSchemas.defaultStringJsonSchema.docs, isOptional = false, documentation = None) :: Nil)) ::
        ("Baz", StrictDocumentedRecord(Field("i", DocumentedJsonSchemas.intJsonSchema.docs, isOptional = false, documentation = None) :: Nil)) ::
        ("Bax", StrictDocumentedRecord(Nil)) ::
        ("Qux", StrictDocumentedRecord(Nil)) ::
        ("Quux", StrictDocumentedRecord(Field("b", DocumentedJsonSchemas.byteJsonSchema.docs, isOptional = false, documentation = None) :: Nil)) ::
        Nil
      )
    assert(DocumentedJsonSchemas.Foo.schema.docs == expectedSchema)
  }

  "enum" in {
    val expectedSchema =
      DocumentedEnum(DocumentedJsonSchemas.defaultStringJsonSchema.docs, ujson.Str("Red") :: ujson.Str("Blue") :: Nil, Some("Color"))
    assert(DocumentedJsonSchemas.Enum.colorSchema.docs == expectedSchema)
  }

  "non-string enum" in {
    val expectedSchema =
      DocumentedEnum(
        StrictDocumentedRecord(
          Field("quux", DocumentedJsonSchemas.defaultStringJsonSchema.docs, isOptional = false, documentation = None) :: Nil
        ),
        ujson.Obj("quux" -> ujson.Str("bar")) :: ujson.Obj("quux" -> ujson.Str("baz")) :: Nil,
        None
      )
    assert(DocumentedJsonSchemas.NonStringEnum.enumSchema.docs == expectedSchema)
  }

  def testStrict(schema: DocumentedJsonSchemas.DocumentedJsonSchema.DocumentedRecord): Assertion = schema match {
    case StrictDocumentedRecord(List(Field("next", tpe, true, None)), None, None, None) => assert(tpe.isInstanceOf[LazyDocumentedRecord])
    case _ => fail(s"Unexpected type for 'recRecordSchema': $schema")
  }

  "recursive record" in {
    testStrict(DocumentedJsonSchemas.recursiveRecordSchema.docs)
  }

  "recursive co-product" in {
    DocumentedJsonSchemas.recursiveTaggedSchema.docs match {
      case lazyCoProduct: LazyDocumentedCoProd =>
        assert(lazyCoProduct.toStrict.discriminatorName == "type")
        assert(lazyCoProduct.toStrict.alternatives.size == 1)
        assert(lazyCoProduct.toStrict.alternatives.head._1 == "RecursiveTagged")
        assert(lazyCoProduct.example.isEmpty)
        testStrict(lazyCoProduct.toStrict.alternatives.head._2)
      case _ =>
        fail(s"Unexpected type for 'recTaggedSchema': ${DocumentedJsonSchemas.recursiveTaggedSchema.docs}")
    }
  }

  "maps" in {
    val expected = StrictDocumentedRecord(Nil, Some(DocumentedJsonSchemas.intJsonSchema.docs), None)
    assert(DocumentedJsonSchemas.intDictionary.docs == expected)
  }

  "tuple" in {
    val expected = Array(Right(DocumentedJsonSchemas.booleanJsonSchema.docs :: DocumentedJsonSchemas.intJsonSchema.docs :: DocumentedJsonSchemas.defaultStringJsonSchema.docs :: Nil))
    assert(DocumentedJsonSchemas.boolIntString.docs == expected)
  }

}
