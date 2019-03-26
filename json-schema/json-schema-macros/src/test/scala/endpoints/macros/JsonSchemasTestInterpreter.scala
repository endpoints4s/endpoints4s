package endpoints.macros

import java.util.UUID

import scala.collection.compat.Factory
import scala.language.{higherKinds, implicitConversions}


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

  def lazySchema[A](schema: => JsonSchema[A], name: String): JsonSchema[A] =
    s"{$name}-> $schema"

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

  def xmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] =
    record.asInstanceOf[Record[B]]

  def xmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] =
    tagged.asInstanceOf[Tagged[B]]

  def xmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
    jsonSchema.asInstanceOf[JsonSchema[B]]

  implicit def uuidJsonSchema: JsonSchema[UUID] = "uuid"

  implicit def stringJsonSchema: JsonSchema[String] = "string"

  implicit def intJsonSchema: JsonSchema[Int] = "int"

  implicit def longJsonSchema: JsonSchema[Long] = "long"

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] = "bigdecimal"

  implicit def floatJsonSchema: JsonSchema[Float] = "float"

  implicit def doubleJsonSchema: JsonSchema[Double] = "double"

  implicit def booleanJsonSchema: JsonSchema[Boolean] = "boolean"

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit jsonSchema: JsonSchema[A],
                                                  factory: Factory[A, C[A]]): JsonSchema[C[A]] =
    s"[$jsonSchema]"

  implicit def mapJsonSchema[A](implicit jsonSchema: String with Tag[A]): JsonSchema[Map[String, A]] =
    s"map[$jsonSchema]"
}
