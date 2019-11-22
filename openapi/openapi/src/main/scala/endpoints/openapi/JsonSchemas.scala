package endpoints.openapi

import java.util.UUID

import endpoints.{PartialInvariantFunctor, Tupler, Validated, algebra}
import endpoints.algebra.{Documentation, Encoder}

import scala.collection.compat._
import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces a JSON schema for
  * a given algebraic data type description.
  *
  * @group interpreters
  */
trait JsonSchemas extends algebra.JsonSchemas with TuplesSchemas {

  lazy val ujsonSchemas: endpoints.ujson.JsonSchemas = endpoints.ujson.JsonSchemas

  import DocumentedJsonSchema._

  class JsonSchema[A](val ujsonSchema: ujsonSchemas.JsonSchema[A], val docs: DocumentedJsonSchema) {
    final def stringCodec: Encoder[A, String] =
      a => ujsonSchema.codec.encode(a).transform(ujson.StringRenderer()).toString
  }
  class Record[A](override val ujsonSchema: ujsonSchemas.Record[A], override val docs: DocumentedRecord) extends JsonSchema[A](ujsonSchema, docs)
  class Tagged[A](override val ujsonSchema: ujsonSchemas.Tagged[A], override val docs: DocumentedCoProd) extends JsonSchema[A](ujsonSchema, docs)
  class Enum[A](override val ujsonSchema: ujsonSchemas.Enum[A], override val docs: DocumentedEnum) extends JsonSchema[A](ujsonSchema, docs)

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

    case class DocumentedEnum(elementType: DocumentedJsonSchema, values: List[ujson.Value], name: Option[String]) extends DocumentedJsonSchema

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
      def xmapPartial[A, B](fa: JsonSchema[A], f: A => Validated[B], g: B => A): JsonSchema[B] =
        new JsonSchema(ujsonSchemas.jsonSchemaPartialInvFunctor.xmapPartial(fa.ujsonSchema, f, g), fa.docs)
      override def xmap[A, B](fa: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
        new JsonSchema(ujsonSchemas.jsonSchemaPartialInvFunctor.xmap(fa.ujsonSchema, f, g), fa.docs)
    }
  implicit def recordPartialInvFunctor: PartialInvariantFunctor[Record] =
    new PartialInvariantFunctor[Record] {
      def xmapPartial[A, B](fa: Record[A], f: A => Validated[B], g: B => A): Record[B] =
        new Record(ujsonSchemas.recordPartialInvFunctor.xmapPartial(fa.ujsonSchema, f, g), fa.docs)
      override def xmap[A, B](fa: Record[A], f: A => B, g: B => A): Record[B] =
        new Record(ujsonSchemas.recordPartialInvFunctor.xmap(fa.ujsonSchema, f, g), fa.docs)
    }
  implicit def taggedPartialInvFunctor: PartialInvariantFunctor[Tagged] =
    new PartialInvariantFunctor[Tagged] {
      def xmapPartial[A, B](fa: Tagged[A], f: A => Validated[B], g: B => A): Tagged[B] =
        new Tagged(ujsonSchemas.taggedPartialInvFunctor.xmapPartial(fa.ujsonSchema, f, g), fa.docs)
      override def xmap[A, B](fa: Tagged[A], f: A => B, g: B => A): Tagged[B] =
        new Tagged(ujsonSchemas.taggedPartialInvFunctor.xmap(fa.ujsonSchema, f, g), fa.docs)
    }

  def enumeration[A](values: Seq[A])(f: A => String)(implicit tpe: JsonSchema[String]): Enum[A] = {
    val ujsonSchema = ujsonSchemas.enumeration(values)(f)(tpe.ujsonSchema)
    val docs = DocumentedEnum(
      tpe.docs,
      values.map(a => ujsonSchema.codec.encode(a)).toList,
      None
    )
    new Enum(ujsonSchema, docs)
  }

  def namedRecord[A](schema: Record[A], name: String): Record[A] =
    new Record(schema.ujsonSchema, schema.docs.copy(name = Some(name)))
  def namedTagged[A](schema: Tagged[A], name: String): Tagged[A] =
    new Tagged(schema.ujsonSchema, schema.docs.copy(name = Some(name)))
  def namedEnum[A](schema: Enum[A], name: String): Enum[A] =
    new Enum(schema.ujsonSchema, schema.docs.copy(name = Some(name)))

  def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A] =
    new JsonSchema(
      ujsonSchemas.lazyRecord(schema.ujsonSchema, name),
      LazySchema(namedRecord(schema, name).docs)
    )
  def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A] =
    new JsonSchema(
      ujsonSchemas.lazyTagged(schema.ujsonSchema, name),
      LazySchema(namedTagged(schema, name).docs)
    )

  def emptyRecord: Record[Unit] =
    new Record(ujsonSchemas.emptyRecord, DocumentedRecord(Nil))

  def field[A](name: String, docs: Documentation)(implicit tpe: JsonSchema[A]): Record[A] =
    new Record(
      ujsonSchemas.field(name, docs)(tpe.ujsonSchema),
      DocumentedRecord(Field(name, tpe.docs, isOptional = false, docs) :: Nil)
    )

  def optField[A](name: String, docs: Documentation)(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    new Record(
      ujsonSchemas.optField(name, docs)(tpe.ujsonSchema),
      DocumentedRecord(Field(name, tpe.docs, isOptional = true, docs) :: Nil)
    )

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged(
      ujsonSchemas.taggedRecord(recordA.ujsonSchema, tag),
      DocumentedCoProd(List(tag -> recordA.docs))
    )

  def withDiscriminatorTagged[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
    new Tagged(
      ujsonSchemas.withDiscriminatorTagged(tagged.ujsonSchema, discriminatorName),
      tagged.docs.copy(discriminatorName = discriminatorName)
    )

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] =
    new Tagged(
      ujsonSchemas.choiceTagged(taggedA.ujsonSchema, taggedB.ujsonSchema),
      DocumentedCoProd(taggedA.docs.alternatives ++ taggedB.docs.alternatives)
    )

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B])(implicit t: Tupler[A, B]): Record[t.Out] =
    new Record(
      ujsonSchemas.zipRecords(recordA.ujsonSchema, recordB.ujsonSchema),
      DocumentedRecord(recordA.docs.fields ++ recordB.docs.fields)
    )

  lazy val uuidJsonSchema: JsonSchema[UUID] =
    new JsonSchema(
      ujsonSchemas.uuidJsonSchema,
      Primitive("string", format = Some("uuid"))
    )

  lazy val stringJsonSchema: JsonSchema[String] =
    new JsonSchema(ujsonSchemas.stringJsonSchema, Primitive("string"))

  lazy val intJsonSchema: JsonSchema[Int] =
    new JsonSchema(ujsonSchemas.intJsonSchema, Primitive("integer", format = Some("int32")))

  lazy val longJsonSchema: JsonSchema[Long] =
    new JsonSchema(ujsonSchemas.longJsonSchema, Primitive("integer", format = Some("int64")))

  lazy val bigdecimalJsonSchema: JsonSchema[BigDecimal] =
    new JsonSchema(ujsonSchemas.bigdecimalJsonSchema, Primitive("number"))

  lazy val floatJsonSchema: JsonSchema[Float] =
    new JsonSchema(ujsonSchemas.floatJsonSchema, Primitive("number", format = Some("float")))

  lazy val doubleJsonSchema: JsonSchema[Double] =
    new JsonSchema(ujsonSchemas.doubleJsonSchema, Primitive("number", format = Some("double")))

  lazy val booleanJsonSchema: JsonSchema[Boolean] =
    new JsonSchema(ujsonSchemas.booleanJsonSchema, Primitive("boolean"))

  lazy val byteJsonSchema: JsonSchema[Byte] =
    new JsonSchema(ujsonSchemas.byteJsonSchema, Primitive("integer"))

  def arrayJsonSchema[C[X] <: Seq[X], A](implicit
    jsonSchema: JsonSchema[A],
    factory: Factory[A, C[A]]
  ): JsonSchema[C[A]] =
    new JsonSchema(
      ujsonSchemas.arrayJsonSchema(jsonSchema.ujsonSchema, factory),
      Array(Left(jsonSchema.docs))
    )

  def mapJsonSchema[A](implicit jsonSchema: JsonSchema[A]): JsonSchema[Map[String, A]] =
    new JsonSchema(
      ujsonSchemas.mapJsonSchema(jsonSchema.ujsonSchema),
      DocumentedRecord(fields = Nil, additionalProperties = Some(jsonSchema.docs))
    )

}
