package endpoints.macros

import scala.collection.generic.CanBuildFrom
import language.higherKinds

object FakeAlgebraJsonSchemas extends ExampleDomain with endpoints.algebra.JsonSchemas {

  class JsonSchema[+A](val schema: String)
  class Record[+A](override val schema: String) extends JsonSchema[A](schema)
  class Tagged[+A](override val schema: String) extends JsonSchema[A](schema)
  class Enum[+A](override val schema: String) extends JsonSchema[A](schema)

  def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: JsonSchema[String]): Enum[A] =
    new Enum(values.map(encode).mkString("<",",",">"))

  def named[A, S[T] <: JsonSchema[T]](schema: S[A], name: String): S[A] =
    new JsonSchema(s"'$name'!(${schema.schema})").asInstanceOf[S[A]]

  def emptyRecord: Record[Unit] =
    new Record("$")

  def field[A](name: String, docs: Option[String])(implicit tpe: JsonSchema[A]): Record[A] =
    new Record(s"$name:${tpe.schema}")

  def optField[A](name: String, docs: Option[String])(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    new Record(s"$name:${tpe.schema}?")

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged(s"${recordA.schema}@$tag")

  def withDiscriminator[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
    new Tagged(s"${tagged.schema}#$discriminatorName")

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] =
    new Tagged(s"${taggedA.schema}|${taggedB.schema}")

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B]): Record[(A, B)] =
    new Record(s"${recordA.schema},${recordB.schema}")

  def invmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] =
    record.asInstanceOf[Record[B]]

  def invmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] =
    tagged.asInstanceOf[Tagged[B]]

  def invmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
    jsonSchema.asInstanceOf[JsonSchema[B]]

  implicit def stringJsonSchema: JsonSchema[String] = new JsonSchema("string")

  implicit def intJsonSchema: JsonSchema[Int] = new JsonSchema("int")

  implicit def longJsonSchema: JsonSchema[Long] = new JsonSchema("long")

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] = new JsonSchema("bigdecimal")

  implicit def floatJsonSchema: JsonSchema[Float] = new JsonSchema("float")

  implicit def doubleJsonSchema: JsonSchema[Double] = new JsonSchema("double")

  implicit def booleanJsonSchema: JsonSchema[Boolean] = new JsonSchema("boolean")

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit jsonSchema: JsonSchema[A],
                                                  cbf: CanBuildFrom[_, A, C[A]]): JsonSchema[C[A]] =
    new JsonSchema(s"[${jsonSchema.schema}]")
}
