package endpoints
package generic

import org.scalatest.FreeSpec

import scala.collection.compat._
import scala.language.higherKinds

class JsonSchemasTest extends FreeSpec {

  trait GenericSchemas extends JsonSchemas {

    case class Foo(bar: String, baz: Int, qux: Option[Boolean])

    object Foo {
      val schema: JsonSchema[Foo] = genericJsonSchema[Foo]
    }

    sealed trait Quux
    case class QuuxA(ss: List[String]) extends Quux
    case class QuuxB(i: Int) extends Quux
    case class QuuxC(b: Boolean) extends Quux
    case class QuuxD() extends Quux
    case object QuuxE extends Quux

    object Quux {
      implicit val schema: JsonSchema[Quux] = genericJsonSchema[Quux]
    }

    sealed trait Doc
    case class DocA(@docs("fieldDocI") i: Int) extends Doc
    case class DocB(
      a: String,
      @docs("fieldDocB") b: Boolean,
      @docs("fieldDocSS") ss: List[String]
    ) extends Doc
    case object DocC extends Doc

    object Doc {
      val schema: JsonSchema[Doc] = genericJsonSchema[Doc]
    }
  }

  object FakeAlgebraJsonSchemas extends GenericSchemas with endpoints.algebra.JsonSchemas {

      type JsonSchema[+A] = String
      type Record[+A] = String
      type Tagged[+A] = String
      type Enum[+A] = String

      def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: String): String =
        s"$tpe"

      def namedRecord[A](schema: Record[A], name: String): Record[A] = s"'$name'!($schema)"
      def namedTagged[A](schema: Tagged[A], name: String): Tagged[A] = s"'$name'!($schema)"
      def namedEnum[A](schema: Enum[A], name: String): Enum[A] = s"'$name'!($schema)"

      def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A] = s"=>'$name'!($schema)"
      def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A] = s"=>'$name'!($schema)"

      def emptyRecord: String =
        "%"

      def field[A](name: String, docs: Option[String])(implicit tpe: String): String =
        s"$name:$tpe${docs.fold("")(doc => s"{$doc}")}"

      def optField[A](name: String, docs: Option[String])(implicit tpe: String): String =
        s"$name:$tpe?${docs.fold("")(doc => s"{$doc}")}"

      def taggedRecord[A](recordA: String, tag: String): String =
        s"$recordA@$tag"

      def withDiscriminator[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
        s"$tagged#$discriminatorName"

      def choiceTagged[A, B](taggedA: String, taggedB: String): String =
        s"$taggedA|$taggedB"

      def zipRecords[A, B](recordA: String, recordB: String): String =
        s"$recordA,$recordB"

      def xmapRecord[A, B](record: String, f: A => B, g: B => A): String = record

      def xmapTagged[A, B](tagged: String, f: A => B, g: B => A): String = tagged

      def xmapJsonSchema[A, B](jsonSchema: String, f: A => B, g: B => A): String = jsonSchema

      lazy val uuidJsonSchema: String = "uuid"

      lazy val stringJsonSchema: String = "string"

      lazy val intJsonSchema: String = "integer"

      lazy val longJsonSchema: String = "integer"

      lazy val bigdecimalJsonSchema: String = "number"

      lazy val floatJsonSchema: String = "number"

      lazy val doubleJsonSchema: String = "number"

      lazy val booleanJsonSchema: String = "boolean"

      lazy val byteJsonSchema: String = "byte"

      def arrayJsonSchema[C[X] <: Seq[X], A](implicit
                                             jsonSchema: String,
                                             factory: Factory[A, C[A]]
                                            ): String = s"[$jsonSchema]"

      def mapJsonSchema[A](implicit
                           jsonSchema: String
                          ): String = s"{$jsonSchema}"
  }

  val ns = "endpoints.generic.JsonSchemasTest.GenericSchemas"

  "case class" in {
    val expectedSchema = s"'$ns.Foo'!(bar:string,baz:integer,qux:boolean?,%)"
    assert(FakeAlgebraJsonSchemas.Foo.schema == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema = s"'$ns.Quux'!(${
      List(
        s"'$ns.QuuxA'!(ss:[string],%)@QuuxA",
        s"'$ns.QuuxB'!(i:integer,%)@QuuxB",
        s"'$ns.QuuxC'!(b:boolean,%)@QuuxC",
        s"'$ns.QuuxD'!(%)@QuuxD",
        s"'$ns.QuuxE'!(%)@QuuxE"
      ).mkString("|")
    })"
    assert(FakeAlgebraJsonSchemas.Quux.schema == expectedSchema)
  }

  "documentations" in {
    val expectedSchema = s"'$ns.Doc'!(${
      List(
        s"'$ns.DocA'!(i:integer{fieldDocI},%)@DocA",
        s"'$ns.DocB'!(a:string,b:boolean{fieldDocB},ss:[string]{fieldDocSS},%)@DocB",
        s"'$ns.DocC'!(%)@DocC"
      ).mkString("|")
    })"
    assert(FakeAlgebraJsonSchemas.Doc.schema == expectedSchema)
  }

}
