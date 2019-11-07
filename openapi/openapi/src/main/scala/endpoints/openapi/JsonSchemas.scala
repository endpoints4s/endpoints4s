package endpoints
package openapi

import endpoints.algebra.Documentation

import scala.collection.compat._
import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces a JSON schema for
  * a given algebraic data type description.
  *
  * @group interpreters
  */
trait JsonSchemas extends endpoints.algebra.JsonSchemas with TuplesSchemas {

  import DocumentedJsonSchema._

  type JsonSchema[+A] = DocumentedJsonSchema
  type Record[+A] = DocumentedRecord
  type Tagged[+A] = DocumentedCoProd
  type Enum[+A] = DocumentedEnum

  sealed trait DocumentedJsonSchema

  object DocumentedJsonSchema {

    case class DocumentedRecord(fields: List[Field], additionalProperties: Option[DocumentedJsonSchema] = None, name: Option[String] = None) extends DocumentedJsonSchema
    case class Field(name: String, tpe: DocumentedJsonSchema, isOptional: Boolean, documentation: Option[String])

    case class DocumentedCoProd(alternatives: List[(String, DocumentedRecord)],
                                name: Option[String] = None,
                                discriminatorName: String = defaultDiscriminatorName) extends DocumentedJsonSchema

    case class Primitive(name: String, format: Option[String] = None) extends DocumentedJsonSchema

    /**
      * @param schema `Left(itemSchema)` for a homogeneous array, or `Right(itemSchemas)` for a heterogeneous array (ie, a tuple)
      */
    case class Array(schema: Either[DocumentedJsonSchema, List[DocumentedJsonSchema]]) extends DocumentedJsonSchema

    case class DocumentedEnum(elementType: DocumentedJsonSchema, values: List[String], name: Option[String]) extends DocumentedJsonSchema

    // A documented JSON schema that is unevaluated unless its `value` is accessed
    sealed trait LazySchema extends DocumentedJsonSchema {
      def value: DocumentedJsonSchema
    }
    object LazySchema {
      def apply(s: => DocumentedJsonSchema): LazySchema =
        new LazySchema {
          lazy val value: DocumentedJsonSchema = s
        }
    }
  }

  implicit def jsonSchemaPartialInvFunctor: PartialInvariantFunctor[JsonSchema] =
    new PartialInvariantFunctor[JsonSchema] {
      def xmapPartial[A, B](fa: DocumentedJsonSchema, f: A => Validated[B], g: B => A): DocumentedJsonSchema = fa
      override def xmap[A, B](fa: DocumentedJsonSchema, f: A => B, g: B => A): DocumentedJsonSchema = fa
    }
  implicit def recordPartialInvFunctor: PartialInvariantFunctor[Record] =
    new PartialInvariantFunctor[Record] {
      def xmapPartial[A, B](fa: DocumentedRecord, f: A => Validated[B], g: B => A): DocumentedRecord = fa
      override def xmap[A, B](fa: DocumentedRecord, f: A => B, g: B => A): DocumentedRecord = fa
    }
  implicit def taggedPartialInvFunctor: PartialInvariantFunctor[Tagged] =
    new PartialInvariantFunctor[Tagged] {
      def xmapPartial[A, B](fa: DocumentedCoProd, f: A => Validated[B], g: B => A): DocumentedCoProd = fa
      override def xmap[A, B](fa: DocumentedCoProd, f: A => B, g: B => A): DocumentedCoProd = fa
    }

  def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: JsonSchema[String]): DocumentedEnum =
    DocumentedEnum(tpe, values.map(encode).toList, None)

  def namedRecord[A](schema: Record[A], name: String): Record[A] =
    schema.copy(name = Some(name))
  def namedTagged[A](schema: Tagged[A], name: String): Tagged[A] =
    schema.copy(name = Some(name))
  def namedEnum[A](schema: Enum[A], name: String): Enum[A] =
    schema.copy(name = Some(name))

  def lazyRecord[A](schema: => DocumentedRecord, name: String): DocumentedJsonSchema =
    LazySchema(namedRecord(schema, name))
  def lazyTagged[A](schema: => DocumentedCoProd, name: String): DocumentedJsonSchema =
    LazySchema(namedTagged(schema, name))

  def emptyRecord: DocumentedRecord =
    DocumentedRecord(Nil)

  def field[A](name: String, docs: Documentation)(implicit tpe: DocumentedJsonSchema): DocumentedRecord =
    DocumentedRecord(Field(name, tpe, isOptional = false, docs) :: Nil)

  def optField[A](name: String, docs: Documentation)(implicit tpe: DocumentedJsonSchema): DocumentedRecord =
    DocumentedRecord(Field(name, tpe, isOptional = true, docs) :: Nil)

  def taggedRecord[A](recordA: DocumentedRecord, tag: String): DocumentedCoProd =
    DocumentedCoProd(List(tag -> recordA))

  def withDiscriminatorTagged[A](tagged: DocumentedCoProd, discriminatorName: String): DocumentedCoProd =
    tagged.copy(discriminatorName = discriminatorName)

  def choiceTagged[A, B](taggedA: DocumentedCoProd, taggedB: DocumentedCoProd): DocumentedCoProd =
    DocumentedCoProd(taggedA.alternatives ++ taggedB.alternatives)

  def zipRecords[A, B](recordA: DocumentedRecord, recordB: DocumentedRecord): DocumentedRecord =
    DocumentedRecord(recordA.fields ++ recordB.fields)

  lazy val uuidJsonSchema: DocumentedJsonSchema = Primitive("string", format = Some("uuid"))

  lazy val stringJsonSchema: DocumentedJsonSchema = Primitive("string")

  lazy val intJsonSchema: DocumentedJsonSchema = Primitive("integer", format = Some("int32"))

  lazy val longJsonSchema: DocumentedJsonSchema = Primitive("integer", format = Some("int64"))

  lazy val bigdecimalJsonSchema: DocumentedJsonSchema = Primitive("number")

  lazy val floatJsonSchema: DocumentedJsonSchema = Primitive("number", format = Some("float"))

  lazy val doubleJsonSchema: DocumentedJsonSchema = Primitive("number", format = Some("double"))

  lazy val booleanJsonSchema: DocumentedJsonSchema = Primitive("boolean")

  lazy val byteJsonSchema: DocumentedJsonSchema = Primitive("integer")

  def arrayJsonSchema[C[X] <: Seq[X], A](implicit
    jsonSchema: JsonSchema[A],
    factory: Factory[A, C[A]]
  ): JsonSchema[C[A]] = Array(Left(jsonSchema))

  def mapJsonSchema[A](implicit jsonSchema: DocumentedJsonSchema): DocumentedJsonSchema =
    DocumentedRecord(fields = Nil, additionalProperties = Some(jsonSchema))

}
