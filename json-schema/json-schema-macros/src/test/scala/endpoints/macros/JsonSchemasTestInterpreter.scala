package endpoints.macros

import scala.collection.generic.CanBuildFrom
import language.higherKinds
import language.implicitConversions


trait JsonSchemasTestInterpreter extends endpoints.algebra.JsonSchemas {

  // use tagged types in tests to avoid ambiguous implicit searches on primitives
  trait Tag[+A]
  implicit def toTagged[A](s: String): String with Tag[A] =
    s.asInstanceOf[String with Tag[A]]

  type JsonSchema[+A] = String with Tag[A]
  type Record[+A] = String with Tag[A]
  type Tagged[+A] = String with Tag[A]
  type Enum[+A] = String with Tag[A]

  def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: JsonSchema[String]): Enum[A] =
    values.map(encode).mkString("<",",",">")

  def named[A, S[T] <: JsonSchema[T]](schema: S[A], name: String): S[A] =
    s"'$name'!($schema)".asInstanceOf[S[A]]

  def emptyRecord: Record[Unit] =
    "$"

  def field[A](name: String, docs: Option[String])(implicit tpe: JsonSchema[A]): Record[A] =
    s"$name:$tpe"

  def optField[A](name: String, docs: Option[String])(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    s"$name:$tpe?"

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    s"$recordA@$tag"

  def withDiscriminator[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
    s"$tagged#$discriminatorName"

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] =
    s"$taggedA|$taggedB"

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B]): Record[(A, B)] =
    s"$recordA,$recordB"

  def invmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] =
    record.asInstanceOf[Record[B]]

  def invmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] =
    tagged.asInstanceOf[Tagged[B]]

  def invmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
    jsonSchema.asInstanceOf[JsonSchema[B]]

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
