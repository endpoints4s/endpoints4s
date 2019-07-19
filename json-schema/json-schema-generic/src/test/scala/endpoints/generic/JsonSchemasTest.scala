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
  }

  object FakeAlgebraJsonSchemas extends GenericSchemas with endpoints.algebra.JsonSchemas {

      type JsonSchema[+A] = String
      type Record[+A] = String
      type Tagged[+A] = String
      type Enum[+A] = String

      def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: String): String =
        s"$tpe"

      def named[A, S[T] <: String](schema: S[A], name: String): S[A] =
        s"'$name'!($schema)".asInstanceOf[S[A]]

      def lazySchema[A](schema: => JsonSchema[A], name: String): JsonSchema[A] =
        s"=>'$name'!($schema)"

      def emptyRecord: String =
        "$"

      def field[A](name: String, docs: Option[String])(implicit tpe: String): String =
        s"$name:$tpe"

      def optField[A](name: String, docs: Option[String])(implicit tpe: String): String =
        s"$name:$tpe?"

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

  "case class" in {
    val expectedSchema = "'endpoints.generic.JsonSchemasTest.GenericSchemas.Foo'!('endpoints.generic.JsonSchemasTest.GenericSchemas.Foo'!(bar:string,baz:integer,qux:boolean?,$))"
    assert(FakeAlgebraJsonSchemas.Foo.schema == expectedSchema)
  }

  "sealed trait" in {
    val expectedSchema = "'endpoints.generic.JsonSchemasTest.GenericSchemas.Quux'!('endpoints.generic.JsonSchemasTest.GenericSchemas.Quux'!('endpoints.generic.JsonSchemasTest.GenericSchemas.QuuxA'!(ss:[string],$)@QuuxA|'endpoints.generic.JsonSchemasTest.GenericSchemas.QuuxB'!(i:integer,$)@QuuxB|'endpoints.generic.JsonSchemasTest.GenericSchemas.QuuxC'!(b:boolean,$)@QuuxC|'endpoints.generic.JsonSchemasTest.GenericSchemas.QuuxD'!($)@QuuxD|'endpoints.generic.JsonSchemasTest.GenericSchemas.QuuxE'!($)@QuuxE))"
    assert(FakeAlgebraJsonSchemas.Quux.schema == expectedSchema)
  }

}
