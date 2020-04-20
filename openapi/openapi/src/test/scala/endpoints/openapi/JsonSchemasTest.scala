package endpoints.openapi

import endpoints.algebra
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasTest extends AnyFreeSpec {

  object DocumentedJsonSchemas
      extends algebra.JsonSchemasFixtures
      with JsonSchemas

  import DocumentedJsonSchemas.DocumentedJsonSchema._

  private val expectedSealedTraitSchema = DocumentedCoProd(
    (
      "Bar",
      DocumentedRecord(
        Field(
          "s",
          DocumentedJsonSchemas.defaultStringJsonSchema.docs,
          isOptional = false,
          documentation = None
        ) :: Nil
      )
    ) ::
      (
        "Baz",
        DocumentedRecord(
          Field(
            "i",
            DocumentedJsonSchemas.intJsonSchema.docs,
            isOptional = false,
            documentation = None
          ) :: Nil
        )
      ) ::
      ("Bax", DocumentedRecord(Nil)) ::
      ("Qux", DocumentedRecord(Nil)) ::
      (
        "Quux",
        DocumentedRecord(
          Field(
            "b",
            DocumentedJsonSchemas.byteJsonSchema.docs,
            isOptional = false,
            documentation = None
          ) :: Nil
        )
      ) ::
      Nil
  )

  "case class" in {
    val expectedSchema =
      DocumentedRecord(
        Field(
          "name",
          DocumentedJsonSchemas.defaultStringJsonSchema.docs,
          isOptional = false,
          documentation = Some("Name of the user")
        ) ::
          Field(
            "age",
            DocumentedJsonSchemas.intJsonSchema.docs,
            isOptional = false,
            documentation = None
          ) ::
          Nil
      )
    assert(DocumentedJsonSchemas.User.schema.docs == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema = expectedSealedTraitSchema
    assert(DocumentedJsonSchemas.Foo.schema.docs == expectedSchema)
  }

  "sealed trait tagged merge" in {
    val expectedSchema = expectedSealedTraitSchema
    assert(
      DocumentedJsonSchemas.Foo.alternativeSchemaForMerge.docs == expectedSchema
    )
  }

  "enum" in {
    val expectedSchema =
      DocumentedEnum(
        DocumentedJsonSchemas.defaultStringJsonSchema.docs,
        ujson.Str("Red") :: ujson.Str("Blue") :: Nil,
        Some("Color")
      )
    assert(DocumentedJsonSchemas.Enum.colorSchema.docs == expectedSchema)
  }

  "non-string enum" in {
    val expectedSchema =
      DocumentedEnum(
        DocumentedRecord(
          Field(
            "quux",
            DocumentedJsonSchemas.defaultStringJsonSchema.docs,
            isOptional = false,
            documentation = None
          ) :: Nil
        ),
        ujson.Obj("quux" -> ujson.Str("bar")) :: ujson.Obj(
          "quux" -> ujson.Str("baz")
        ) :: Nil,
        None,
        None,
        None
      )
    assert(
      DocumentedJsonSchemas.NonStringEnum.enumSchema.docs == expectedSchema
    )
  }

  "recursive" in {
    DocumentedJsonSchemas.recursiveSchema.docs match {
      case DocumentedRecord(
          List(Field("next", tpe, true, None)),
          None,
          None,
          None,
          None,
          None
          ) =>
        assert(tpe.isInstanceOf[LazySchema])
      case _ =>
        fail(
          s"Unexpected type for 'recSchema': ${DocumentedJsonSchemas.recursiveSchema.docs}"
        )
    }
  }

  "maps" in {
    val expected = DocumentedRecord(
      Nil,
      Some(DocumentedJsonSchemas.intJsonSchema.docs),
      None
    )
    assert(DocumentedJsonSchemas.intDictionary.docs == expected)
  }

  "tuple" in {
    val expected = Array(
      Right(
        DocumentedJsonSchemas.booleanJsonSchema.docs :: DocumentedJsonSchemas.intJsonSchema.docs :: DocumentedJsonSchemas.defaultStringJsonSchema.docs :: Nil
      )
    )
    assert(DocumentedJsonSchemas.boolIntString.docs == expected)
  }

}
