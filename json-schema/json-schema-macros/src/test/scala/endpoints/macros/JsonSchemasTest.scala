package endpoints.macros

import scala.collection.generic.CanBuildFrom
import org.scalatest.FreeSpec

import scala.language.higherKinds

class JsonSchemasTest extends FreeSpec {

  import FakeAlgebraJsonSchemas._

  "macros derive JsonSchema for primitives" in {

    assert(genericJsonSchema[String] == "string")
    assert(genericJsonSchema[Int] == "int")
    assert(genericJsonSchema[Long] == "long")
    assert(genericJsonSchema[BigDecimal] == "bigdecimal")
    assert(genericJsonSchema[Float] == "float")
    assert(genericJsonSchema[Double] == "double")
    assert(genericJsonSchema[Boolean] == "boolean")
  }

  "macros derive JsonSchema for sequence types" in {

    assert(listIntSchema == "[int]")
    assert(seqFooSchema == "['endpoints.macros.GenericSchemas.Foo'!(bar:string,baz:int,qux:boolean?)]")
  }

  "macros derive JsonSchema for records" in {

    assert(Foo.schema == "'endpoints.macros.GenericSchemas.Foo'!(bar:string,baz:int,qux:boolean?)")

    assert(QuuxA.schema == "'endpoints.macros.GenericSchemas.QuuxA'!(ss:['endpoints.macros.GenericSchemas.Foo'!(bar:string,baz:int,qux:boolean?)])")
    assert(QuuxB.schema == "'endpoints.macros.GenericSchemas.QuuxB'!(i:int)")
    assert(QuuxC.schema == "'endpoints.macros.GenericSchemas.QuuxC'!(b:boolean)")
    assert(QuuxD.schema == "'endpoints.macros.GenericSchemas.QuuxD'!($)")
    assert(QuuxE.schema == "'endpoints.macros.GenericSchemas.QuuxE'!($)")
  }

  "macros derive JsonSchema for sum types" in {

    assert(SingleCaseBase.schema == "'endpoints.macros.GenericSchemas.SingleCaseBase'!('endpoints.macros.GenericSchemas.SingleCaseInst'!(foo:string)@SingleCaseInst)")
    assert(Quux.schema == "'endpoints.macros.GenericSchemas.Quux'!('endpoints.macros.GenericSchemas.QuuxA'!(ss:['endpoints.macros.GenericSchemas.Foo'!(bar:string,baz:int,qux:boolean?)])@QuuxA|'endpoints.macros.GenericSchemas.QuuxB'!(i:int)@QuuxB|'endpoints.macros.GenericSchemas.QuuxC'!(b:boolean)@QuuxC|'endpoints.macros.GenericSchemas.QuuxD'!($)@QuuxD|'endpoints.macros.GenericSchemas.QuuxE'!($)@QuuxE)")
  }

  "macros derive JsonSchema for generic types" in {

    assert(Id.schema[Int] == "string")
    assert(Id.schema[Float] == "string")
    assert(Id.schema[Foo] == "string")

    assert(User.schema[Int] == "'endpoints.macros.GenericSchemas.User'!(id:'endpoints.macros.GenericSchemas.Id'!(id:string),name:string)")
    assert(User.schema[Float] == "'endpoints.macros.GenericSchemas.User'!(id:'endpoints.macros.GenericSchemas.Id'!(id:string),name:string)")
    assert(User.schema[Foo] == "'endpoints.macros.GenericSchemas.User'!(id:'endpoints.macros.GenericSchemas.Id'!(id:string),name:string)")
  }
}

trait GenericSchemas extends JsonSchemas {

  case class Foo(bar: String, baz: Int, qux: Option[Boolean])

  object Foo {
    val schema: JsonSchema[Foo] = genericJsonSchema[Foo]
  }

  sealed trait Quux

  case class QuuxA(ss: List[Foo]) extends Quux
  object QuuxA {
    implicit val schema: JsonSchema[QuuxA] = genericJsonSchema[QuuxA]
  }

  case class QuuxB(i: Int) extends Quux
  object QuuxB {
    implicit val schema: JsonSchema[QuuxB] = genericJsonSchema[QuuxB]
  }

  case class QuuxC(b: Boolean) extends Quux
  object QuuxC {
    implicit val schema: JsonSchema[QuuxC] = genericJsonSchema[QuuxC]
  }

  case class QuuxD() extends Quux
  object QuuxD {
    implicit val schema: JsonSchema[QuuxD] = genericJsonSchema[QuuxD]
  }

  case object QuuxE extends Quux {
    implicit val schema: JsonSchema[QuuxE.type] = genericJsonSchema[QuuxE.type]
  }

  object Quux {
    implicit val schema: JsonSchema[Quux] = genericJsonSchema[Quux]
  }

  sealed trait SingleCaseBase
  case class SingleCaseInst(foo: String) extends SingleCaseBase

  object SingleCaseBase {
    implicit val schema: JsonSchema[SingleCaseBase] = genericJsonSchema[SingleCaseBase]
  }

  val listIntSchema: JsonSchema[List[Int]] = genericJsonSchema[List[Int]]
  val seqFooSchema: JsonSchema[Seq[Foo]] = genericJsonSchema[Seq[Foo]]

  case class Id[T](id: String)
  object Id {
    implicit def schema[T]: JsonSchema[Id[T]] =
      stringJsonSchema.invmap(Id.apply[T] _)(_.id)
  }

  case class User[T](id: Id[T], name: String)
  object User {
    implicit def schema[T]: JsonSchema[User[T]] = genericJsonSchema[User[T]]
  }
}

object FakeAlgebraJsonSchemas extends GenericSchemas with endpoints.algebra.JsonSchemas {

  type JsonSchema[+A] = String
  type Record[+A] = String
  type Tagged[+A] = String

  override def named[A, S[T] <: String](schema: S[A], name: String): S[A] =
    s"'$name'!($schema)".asInstanceOf[S[A]]

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

  def invmapRecord[A, B](record: String, f: A => B, g: B => A): String = record

  def invmapTagged[A, B](tagged: String, f: A => B, g: B => A): String = tagged

  def invmapJsonSchema[A, B](jsonSchema: String, f: A => B, g: B => A): String = jsonSchema

  lazy val stringJsonSchema: String = "string"

  lazy val intJsonSchema: String = "int"

  lazy val longJsonSchema: String = "long"

  lazy val bigdecimalJsonSchema: String = "bigdecimal"

  lazy val floatJsonSchema: String = "float"

  lazy val doubleJsonSchema: String = "double"

  lazy val booleanJsonSchema: String = "boolean"

  def arrayJsonSchema[C[X] <: Seq[X], A](implicit jsonSchema: String,
                                         cbf: CanBuildFrom[_, A, C[A]]): String =
    s"[$jsonSchema]"
}
