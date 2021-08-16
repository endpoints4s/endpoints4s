package endpoints4s.openapi

import endpoints4s.algebra
import org.scalatest.freespec.AnyFreeSpec

class JsonSchemasTest extends AnyFreeSpec {

  object DocumentedJsonSchemas extends algebra.JsonSchemasFixtures with JsonSchemas

  import DocumentedJsonSchemas.DocumentedJsonSchema._

  private val expectedSealedTraitSchema = DocumentedCoProd(
    (
      "Bar",
      DocumentedRecord(
        Field(
          "s",
          DocumentedJsonSchemas.defaultStringJsonSchema.docs,
          isOptional = false,
          defaultValue = None,
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
            defaultValue = None,
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
            defaultValue = None,
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
          defaultValue = None,
          documentation = Some("Name of the user")
        ) ::
          Field(
            "age",
            DocumentedJsonSchemas.intJsonSchema.docs,
            isOptional = false,
            defaultValue = None,
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
            defaultValue = None,
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
    def badSchema =
      fail(s"Unexpected type for 'recursiveSchema': ${DocumentedJsonSchemas.recursiveSchema.docs}")

    DocumentedJsonSchemas.recursiveSchema.docs match {
      case r: DocumentedRecord.Lazy =>
        assert(r.name == Some("Rec"))
        assert(r.additionalProperties == None)
        assert(r.description == Some("Rec description"))
        assert(r.example == Some(ujson.Obj()))
        assert(r.title == Some("Rec title"))
        r.fields match {
          case List(field) =>
            assert(field.name == "next")
            assert(field.tpe eq r)
            assert(field.isOptional == true)
            assert(field.default == None)
            assert(field.documentation == None)
          case _ => badSchema

        }
      case _ => badSchema
    }
  }

  "recursive expression" in {
    DocumentedJsonSchemas.expressionSchema.docs match {
      case r: LazySchema =>
        assert(r.name == "Expression")
        assert(
          r.value == OneOf(
            List(
              Primitive(
                "integer",
                Some("int32"),
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None
              ),
              DocumentedRecord(
                List(
                  Field("x", r, false, None, None),
                  Field("y", r, false, None, None)
                ),
                None,
                None,
                None,
                None,
                None
              )
            ),
            Some("Expression description"),
            Some(ujson.Num(1)),
            Some("Expression title")
          )
        )
      case _ =>
        fail(
          s"Unexpected type for 'expressionSchema': ${DocumentedJsonSchemas.expressionSchema.docs}"
        )
    }
  }
  "mutually recursive" in {
    DocumentedJsonSchemas.mutualRecursiveA.docs match {
      case r: LazySchema =>
        assert(r.name == "MutualRecursiveA")
        assert(
          r.value == DocumentedRecord(
            List(Field("b", DocumentedJsonSchemas.mutualRecursiveB.docs, true, None, None)),
            None,
            None,
            None,
            None,
            None
          )
        )
      case _ =>
        fail(
          s"Unexpected type for 'mutualRecursiveA': ${DocumentedJsonSchemas.mutualRecursiveA.docs}"
        )
    }
  }
  "tagged recursive" in {
    def badSchema =
      fail(
        s"Unexpected type for 'taggedRecursiveSchema': ${DocumentedJsonSchemas.taggedRecursiveSchema.docs}"
      )

    DocumentedJsonSchemas.taggedRecursiveSchema.docs match {
      case r: DocumentedCoProd.Lazy =>
        assert(r.name == Some("TaggedRec"))
        assert(r.discriminatorName == "kind")
        assert(r.description == Some("TaggedRec description"))
        assert(r.example == Some(ujson.Obj("a" -> ujson.Str("foo"), "kind" -> ujson.Str("A"))))
        assert(r.title == Some("TaggedRec title"))
        r.alternatives match {
          case ("A", a) :: ("B", b) :: Nil =>
            val expectedNext = Field(
              "next",
              r,
              isOptional = true,
              defaultValue = None,
              documentation = None
            )
            val expectedA = DocumentedRecord(
              Field(
                "a",
                DocumentedJsonSchemas.defaultStringJsonSchema.docs,
                isOptional = false,
                defaultValue = None,
                documentation = None
              )
                :: expectedNext
                :: Nil
            )
            val expectedB = DocumentedRecord(
              Field(
                "b",
                DocumentedJsonSchemas.intJsonSchema.docs,
                isOptional = false,
                defaultValue = None,
                documentation = None
              )
                :: expectedNext
                :: Nil
            )
            assert(a == expectedA)
            assert(b == expectedB)
          case _ => badSchema
        }
      case _ => badSchema
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

  "constrained numeric value" in {
    val expected = Primitive(
      name = "integer",
      format = Some("int32"),
      minimum = Some(0.0),
      maximum = Some(10.0),
      exclusiveMaximum = Some(true),
      multipleOf = Some(2.0)
    )

    assert(DocumentedJsonSchemas.constraintNumericSchema.docs == expected)
  }

  "optional field with default" in {
    val expectedSchema =
      DocumentedRecord(
        Field(
          "name",
          DocumentedJsonSchemas.defaultStringJsonSchema.docs,
          isOptional = false,
          defaultValue = None,
          documentation = Some("Name of the user")
        ) ::
          Field(
            "age",
            DocumentedJsonSchemas.intJsonSchema.docs,
            isOptional = true,
            defaultValue = Some(ujson.Num(42)),
            documentation = None
          ) ::
          Nil
      )
    assert(DocumentedJsonSchemas.User.schemaWithDefault.docs == expectedSchema)
  }
}
