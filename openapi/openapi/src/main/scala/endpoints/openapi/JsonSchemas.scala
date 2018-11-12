package endpoints
package openapi

import endpoints.algebra.Documentation

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces a JSON schema for
  * a given algebraic data type description.
  *
  * @group interpreters
  */
trait JsonSchemas extends endpoints.algebra.JsonSchemas {

  import DocumentedJsonSchema._

  type JsonSchema[+A] = DocumentedJsonSchema
  type Record[+A] = DocumentedRecord
  type Tagged[+A] = DocumentedCoProd
  type Enum[+A] = DocumentedEnum

  sealed trait DocumentedJsonSchema

  object DocumentedJsonSchema {

    case class DocumentedRecord(fields: List[Field], name: Option[String] = None) extends DocumentedJsonSchema
    case class Field(name: String, tpe: DocumentedJsonSchema, isOptional: Boolean, documentation: Option[String])

    case class DocumentedCoProd(alternatives: List[(String, DocumentedRecord)],
                                name: Option[String] = None,
                                discriminatorName: String = defaultDiscriminatorName) extends DocumentedJsonSchema

    case class Primitive(name: String, format: Option[String] = None) extends DocumentedJsonSchema

    case class Array(elementType: DocumentedJsonSchema) extends DocumentedJsonSchema

    case class DocumentedEnum(elementType: DocumentedJsonSchema, values: Seq[String]) extends DocumentedJsonSchema
  }

  def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: JsonSchema[String]): DocumentedEnum =
    DocumentedEnum(tpe, values.map(encode))

  override def named[A, S[_] <: DocumentedJsonSchema](schema: S[A], name: String): S[A] = {
    import DocumentedJsonSchema._
    schema match {
      case record: DocumentedRecord =>
        record.copy(name = Some(name)).asInstanceOf[S[A]]
      case coprod: DocumentedCoProd =>
        coprod.copy(name = Some(name)).asInstanceOf[S[A]]
      case other =>
        other
    }
  }

  def emptyRecord: DocumentedRecord =
    DocumentedRecord(Nil)

  def field[A](name: String, docs: Documentation)(implicit tpe: DocumentedJsonSchema): DocumentedRecord =
    DocumentedRecord(Field(name, tpe, isOptional = false, docs) :: Nil)

  def optField[A](name: String, docs: Documentation)(implicit tpe: DocumentedJsonSchema): DocumentedRecord =
    DocumentedRecord(Field(name, tpe, isOptional = true, docs) :: Nil)

  def taggedRecord[A](recordA: DocumentedRecord, tag: String): DocumentedCoProd =
    DocumentedCoProd(List(tag -> recordA))

  def withDiscriminator[A](tagged: DocumentedCoProd, discriminatorName: String): DocumentedCoProd =
    tagged.copy(discriminatorName = discriminatorName)

  def choiceTagged[A, B](taggedA: DocumentedCoProd, taggedB: DocumentedCoProd): DocumentedCoProd =
    DocumentedCoProd(taggedA.alternatives ++ taggedB.alternatives)

  def zipRecords[A, B](recordA: DocumentedRecord, recordB: DocumentedRecord): DocumentedRecord =
    DocumentedRecord(recordA.fields ++ recordB.fields)

  def invmapRecord[A, B](record: DocumentedRecord, f: A => B, g: B => A): DocumentedRecord = record

  def invmapTagged[A, B](tagged: DocumentedCoProd, f: A => B, g: B => A): DocumentedCoProd = tagged

  def invmapJsonSchema[A, B](jsonSchema: DocumentedJsonSchema, f: A => B, g: B => A): DocumentedJsonSchema = jsonSchema

  lazy val stringJsonSchema: DocumentedJsonSchema = Primitive("string")

  lazy val intJsonSchema: DocumentedJsonSchema = Primitive("integer", format = Some("int32"))

  lazy val longJsonSchema: DocumentedJsonSchema = Primitive("integer", format = Some("int64"))

  lazy val bigdecimalJsonSchema: DocumentedJsonSchema = Primitive("number")

  lazy val floatJsonSchema: DocumentedJsonSchema = Primitive("number", format = Some("float"))

  lazy val doubleJsonSchema: DocumentedJsonSchema = Primitive("number", format = Some("double"))

  lazy val booleanJsonSchema: DocumentedJsonSchema = Primitive("boolean")

  def arrayJsonSchema[C[X] <: Seq[X], A](implicit
    jsonSchema: JsonSchema[A],
    cbf: CanBuildFrom[_, A, C[A]]
  ): JsonSchema[C[A]] = Array(jsonSchema)
}
