package endpoints
package generic

import org.scalatest.FreeSpec

import scala.reflect.ClassTag

class JsonSchemasTest extends FreeSpec {

  trait GenericSchemas extends JsonSchemas {

    case class Foo(bar: String, baz: Int, qux: Option[Boolean])

    object Foo {
      val schema: JsonSchema[Foo] = genericJsonSchema[Foo]
    }

    sealed trait Quux
    case class QuuxA(s: String) extends Quux
    case class QuuxB(i: Int) extends Quux
    case class QuuxC(b: Boolean) extends Quux
    case class QuuxD() extends Quux
    case object QuuxE extends Quux

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
        Field("qux", DocumentedGenericSchemas.booleanJsonSchema, isOptional = true, documentation = None) ::
        Nil,
        Some(implicitly[ClassTag[DocumentedGenericSchemas.Foo]].runtimeClass.getName)
      )
    assert(DocumentedGenericSchemas.Foo.schema == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema =
      DocumentedCoProd(
        ("QuuxA", DocumentedRecord(
          Field("s", DocumentedGenericSchemas.stringJsonSchema, isOptional = false, documentation = None) :: Nil,
          Some(implicitly[ClassTag[DocumentedGenericSchemas.QuuxA]].runtimeClass.getName))
        ) ::
        ("QuuxB", DocumentedRecord(
          Field("i", DocumentedGenericSchemas.intJsonSchema, isOptional = false, documentation = None) :: Nil,
          Some(implicitly[ClassTag[DocumentedGenericSchemas.QuuxB]].runtimeClass.getName))
        ) ::
        ("QuuxC", DocumentedRecord(
          Field("b", DocumentedGenericSchemas.booleanJsonSchema, isOptional = false, documentation = None) :: Nil,
          Some(implicitly[ClassTag[DocumentedGenericSchemas.QuuxC]].runtimeClass.getName))
        ) ::
        ("QuuxD", DocumentedRecord(Nil, Some(implicitly[ClassTag[DocumentedGenericSchemas.QuuxD]].runtimeClass.getName))) ::
        ("QuuxE", DocumentedRecord(Nil, Some(implicitly[ClassTag[DocumentedGenericSchemas.QuuxE.type]].runtimeClass.getName))) ::
        Nil,
        Some(implicitly[ClassTag[DocumentedGenericSchemas.Quux]].runtimeClass.getName)
      )
    assert(DocumentedGenericSchemas.Quux.schema == expectedSchema)
  }

}
