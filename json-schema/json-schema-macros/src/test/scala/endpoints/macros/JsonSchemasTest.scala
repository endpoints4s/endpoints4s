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

//    assert(listIntSchema == "[int]")
//    assert(seqFooSchema == "['endpoints.macros.GenericSchemas.Foo'!(bar:string,baz:int,qux:boolean?)]")
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
    implicit val schema: JsonSchema[Foo] = genericJsonSchema[Foo]
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

//  val listIntSchema: JsonSchema[List[Int]] = genericJsonSchema[List[Int]]
//  val seqFooSchema: JsonSchema[List[Foo]] = genericJsonSchema[List[Foo]]

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

  import language.implicitConversions

  trait Tag[+A]
  implicit def autoTag[T](s: String): String with Tag[T] =
    s.asInstanceOf[String with Tag[T]]

  type JsonSchema[+A] = String with Tag[A]
  type Record[+A] = String with Tag[A]
  type Tagged[+A] = String with Tag[A]

  def named[A, S[T] <: String](schema: S[A], name: String): S[A] =
    s"'$name'!($schema)".asInstanceOf[S[A]]

  def emptyRecord: Record[Unit] =
    "$"

  def field[A](name: String, docs: Option[String])(implicit tpe: JsonSchema[A]): Record[A] =
    s"$name:$tpe"

  def optField[A](name: String, docs: Option[String])(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    s"$name:$tpe?"

  def taggedRecord[A](recordA: Record[A], tag: String): Record[A] =
    s"$recordA@$tag"

  def withDiscriminator[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
    s"$tagged#$discriminatorName"

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] =
    s"$taggedA|$taggedB"

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B]): Record[(A, B)] =
    s"$recordA,$recordB"

  def invmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] = record

  def invmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] = tagged

  def invmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] = jsonSchema

  implicit def stringJsonSchema: JsonSchema[String] = "string"

  implicit def intJsonSchema: JsonSchema[Int] = "int"

  implicit def longJsonSchema: JsonSchema[Long] = "long"

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] = "bigdecimal"

  implicit def floatJsonSchema: JsonSchema[Float] = "float"

  implicit def doubleJsonSchema: JsonSchema[Double] = "double"

  implicit def booleanJsonSchema: JsonSchema[Boolean] = "boolean"

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit jsonSchema: JsonSchema[A],
                                                  cbf: CanBuildFrom[_, A, C[A]]): JsonSchema[C[A]] =
    s"[$jsonSchema]"
}
