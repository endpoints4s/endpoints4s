package endpoints
package generic

import org.scalatest.FreeSpec

class JsonSchemasTest extends FreeSpec {

  trait GenericSchemas extends JsonSchemas {

    case class Foo(bar: String, baz: Int)

    object Foo {
      val schema: JsonSchema[Foo] = genericJsonSchema[Foo]
    }

    sealed trait Quux
    case class QuuxA(s: String) extends Quux
    case class QuuxB(i: Int) extends Quux
    case class QuuxC(b: Boolean) extends Quux

    object Quux {
      implicit val schema: JsonSchema[Quux] = genericJsonSchema[Quux]
    }
  }

  object DocumentedGenericSchemas extends GenericSchemas with openapi.JsonSchemas

  import DocumentedGenericSchemas.DocumentedJsonSchema._

  "case class" in {
    val expectedSchema =
      DocumentedRecord(
        Field("bar", DocumentedGenericSchemas.stringJsonSchema, isOptional = false, documentation = None) ::
        Field("baz", DocumentedGenericSchemas.intJsonSchema, isOptional = false, documentation = None) ::
        Nil
      )
    assert(DocumentedGenericSchemas.Foo.schema == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema =
      DocumentedCoProd(
        ("QuuxA", DocumentedRecord(Field("s", DocumentedGenericSchemas.stringJsonSchema, isOptional = false, documentation = None) :: Nil)) ::
        ("QuuxB", DocumentedRecord(Field("i", DocumentedGenericSchemas.intJsonSchema, isOptional = false, documentation = None) :: Nil)) ::
        ("QuuxC", DocumentedRecord(Field("b", DocumentedGenericSchemas.booleanJsonSchema, isOptional = false, documentation = None) :: Nil)) ::
        Nil
      )
    assert(DocumentedGenericSchemas.Quux.schema == expectedSchema)
  }

}
