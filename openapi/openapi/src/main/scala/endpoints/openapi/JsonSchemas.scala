package endpoints.openapi

import endpoints.{PartialInvariantFunctor, Tupler, Validated, algebra}
import endpoints.algebra.Documentation
import endpoints.openapi.model.Schema
import endpoints.openapi.model.Schema.{
  DiscriminatedAlternatives,
  EnumeratedAlternatives
}

import scala.collection.compat._

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces a JSON schema for
  * a given algebraic data type description.
  *
  * @group interpreters
  */
trait JsonSchemas extends algebra.JsonSchemas with TuplesSchemas {
  openapiJsonSchemas =>

  /**
    * The JSON codecs used to produce some parts of the documentation.
    */
  lazy val ujsonSchemas: endpoints.ujson.JsonSchemas =
    new endpoints.ujson.JsonSchemas {
      override def defaultDiscriminatorName: String =
        openapiJsonSchemas.defaultDiscriminatorName
    }

  import DocumentedJsonSchema._

  class JsonSchema[A](
      val ujsonSchema: ujsonSchemas.JsonSchema[A],
      val docs: DocumentedJsonSchema
  )
  class Record[A](
      override val ujsonSchema: ujsonSchemas.Record[A],
      override val docs: DocumentedRecord
  ) extends JsonSchema[A](ujsonSchema, docs)
  class Tagged[A](
      override val ujsonSchema: ujsonSchemas.Tagged[A],
      override val docs: DocumentedCoProd
  ) extends JsonSchema[A](ujsonSchema, docs)
  class Enum[A](
      override val ujsonSchema: ujsonSchemas.Enum[A],
      override val docs: DocumentedEnum
  ) extends JsonSchema[A](ujsonSchema, docs)

  sealed trait DocumentedJsonSchema {
    def description: Option[String]
    def example: Option[ujson.Value]
    def title: Option[String]
  }

  object DocumentedJsonSchema {

    case class DocumentedRecord(
        fields: List[Field],
        additionalProperties: Option[DocumentedJsonSchema] = None,
        name: Option[String] = None,
        description: Option[String] = None,
        example: Option[ujson.Value] = None,
        title: Option[String] = None
    ) extends DocumentedJsonSchema
    case class Field(
        name: String,
        tpe: DocumentedJsonSchema,
        isOptional: Boolean,
        documentation: Option[String]
    )

    case class DocumentedCoProd(
        alternatives: List[(String, DocumentedRecord)],
        name: Option[String] = None,
        discriminatorName: String = defaultDiscriminatorName,
        description: Option[String] = None,
        example: Option[ujson.Value] = None,
        title: Option[String] = None
    ) extends DocumentedJsonSchema

    case class Primitive(
        name: String,
        format: Option[String] = None,
        description: Option[String] = None,
        example: Option[ujson.Value] = None,
        title: Option[String] = None
    ) extends DocumentedJsonSchema

    /**
      * @param schema `Left(itemSchema)` for a homogeneous array, or `Right(itemSchemas)` for a heterogeneous array (ie, a tuple)
      */
    case class Array(
        schema: Either[DocumentedJsonSchema, List[DocumentedJsonSchema]],
        description: Option[String] = None,
        example: Option[ujson.Value] = None,
        title: Option[String] = None
    ) extends DocumentedJsonSchema

    case class DocumentedEnum(
        elementType: DocumentedJsonSchema,
        values: List[ujson.Value],
        name: Option[String],
        description: Option[String] = None,
        example: Option[ujson.Value] = None,
        title: Option[String] = None
    ) extends DocumentedJsonSchema

    // A documented JSON schema that is unevaluated unless its `value` is accessed
    sealed abstract class LazySchema extends DocumentedJsonSchema {
      def value: DocumentedJsonSchema
    }
    object LazySchema {
      def apply(s: => DocumentedJsonSchema): LazySchema =
        new LazySchema {
          lazy val value: DocumentedJsonSchema = s
          def description: Option[String] = value.description
          def example: Option[ujson.Value] = value.example
          def title: Option[String] = value.title
        }
    }

    case class OneOf(
        alternatives: List[DocumentedJsonSchema],
        description: Option[String] = None,
        example: Option[ujson.Value] = None,
        title: Option[String] = None
    ) extends DocumentedJsonSchema
  }

  implicit def jsonSchemaPartialInvFunctor
      : PartialInvariantFunctor[JsonSchema] =
    new PartialInvariantFunctor[JsonSchema] {
      def xmapPartial[A, B](
          fa: JsonSchema[A],
          f: A => Validated[B],
          g: B => A
      ): JsonSchema[B] =
        new JsonSchema(
          ujsonSchemas.jsonSchemaPartialInvFunctor
            .xmapPartial(fa.ujsonSchema, f, g),
          fa.docs
        )
      override def xmap[A, B](
          fa: JsonSchema[A],
          f: A => B,
          g: B => A
      ): JsonSchema[B] =
        new JsonSchema(
          ujsonSchemas.jsonSchemaPartialInvFunctor.xmap(fa.ujsonSchema, f, g),
          fa.docs
        )
    }
  implicit def recordPartialInvFunctor: PartialInvariantFunctor[Record] =
    new PartialInvariantFunctor[Record] {
      def xmapPartial[A, B](
          fa: Record[A],
          f: A => Validated[B],
          g: B => A
      ): Record[B] =
        new Record(
          ujsonSchemas.recordPartialInvFunctor
            .xmapPartial(fa.ujsonSchema, f, g),
          fa.docs
        )
      override def xmap[A, B](fa: Record[A], f: A => B, g: B => A): Record[B] =
        new Record(
          ujsonSchemas.recordPartialInvFunctor.xmap(fa.ujsonSchema, f, g),
          fa.docs
        )
    }
  implicit def taggedPartialInvFunctor: PartialInvariantFunctor[Tagged] =
    new PartialInvariantFunctor[Tagged] {
      def xmapPartial[A, B](
          fa: Tagged[A],
          f: A => Validated[B],
          g: B => A
      ): Tagged[B] =
        new Tagged(
          ujsonSchemas.taggedPartialInvFunctor
            .xmapPartial(fa.ujsonSchema, f, g),
          fa.docs
        )
      override def xmap[A, B](fa: Tagged[A], f: A => B, g: B => A): Tagged[B] =
        new Tagged(
          ujsonSchemas.taggedPartialInvFunctor.xmap(fa.ujsonSchema, f, g),
          fa.docs
        )
    }

  def enumeration[A](values: Seq[A])(tpe: JsonSchema[A]): Enum[A] = {
    val ujsonSchema = ujsonSchemas.enumeration(values)(tpe.ujsonSchema)
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

  def field[A](name: String, docs: Documentation)(
      implicit tpe: JsonSchema[A]
  ): Record[A] =
    new Record(
      ujsonSchemas.field(name, docs)(tpe.ujsonSchema),
      DocumentedRecord(Field(name, tpe.docs, isOptional = false, docs) :: Nil)
    )

  def optField[A](name: String, docs: Documentation)(
      implicit tpe: JsonSchema[A]
  ): Record[Option[A]] =
    new Record(
      ujsonSchemas.optField(name, docs)(tpe.ujsonSchema),
      DocumentedRecord(Field(name, tpe.docs, isOptional = true, docs) :: Nil)
    )

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged(
      ujsonSchemas.taggedRecord(recordA.ujsonSchema, tag),
      DocumentedCoProd(List(tag -> recordA.docs))
    )

  def withDiscriminatorTagged[A](
      tagged: Tagged[A],
      discriminatorName: String
  ): Tagged[A] =
    new Tagged(
      ujsonSchemas
        .withDiscriminatorTagged(tagged.ujsonSchema, discriminatorName),
      tagged.docs.copy(discriminatorName = discriminatorName)
    )

  def choiceTagged[A, B](
      taggedA: Tagged[A],
      taggedB: Tagged[B]
  ): Tagged[Either[A, B]] =
    new Tagged(
      ujsonSchemas.choiceTagged(taggedA.ujsonSchema, taggedB.ujsonSchema),
      DocumentedCoProd(taggedA.docs.alternatives ++ taggedB.docs.alternatives)
    )

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B])(
      implicit t: Tupler[A, B]
  ): Record[t.Out] =
    new Record(
      ujsonSchemas.zipRecords(recordA.ujsonSchema, recordB.ujsonSchema),
      DocumentedRecord(recordA.docs.fields ++ recordB.docs.fields)
    )

  def withExampleJsonSchema[A](
      schema: JsonSchema[A],
      example: A
  ): JsonSchema[A] = {
    val exampleJson = schema.ujsonSchema.codec.encode(example)
    def updatedDocs(djs: DocumentedJsonSchema): DocumentedJsonSchema =
      djs match {
        case s: DocumentedRecord => s.copy(example = Some(exampleJson))
        case s: DocumentedCoProd => s.copy(example = Some(exampleJson))
        case s: Primitive        => s.copy(example = Some(exampleJson))
        case s: Array            => s.copy(example = Some(exampleJson))
        case s: DocumentedEnum   => s.copy(example = Some(exampleJson))
        case s: LazySchema       => LazySchema(updatedDocs(s.value))
        case s: OneOf            => s.copy(example = Some(exampleJson))
      }
    new JsonSchema(
      schema.ujsonSchema,
      updatedDocs(schema.docs)
    )
  }

  def withTitleJsonSchema[A](
      schema: JsonSchema[A],
      title: String
  ): JsonSchema[A] = {
    def updatedDocs(djs: DocumentedJsonSchema): DocumentedJsonSchema =
      djs match {
        case s: DocumentedRecord => s.copy(title = Some(title))
        case s: DocumentedCoProd => s.copy(title = Some(title))
        case s: Primitive        => s.copy(title = Some(title))
        case s: Array            => s.copy(title = Some(title))
        case s: DocumentedEnum   => s.copy(title = Some(title))
        case s: LazySchema       => LazySchema(updatedDocs(s.value))
        case s: OneOf            => s.copy(title = Some(title))
      }
    new JsonSchema(
      schema.ujsonSchema,
      updatedDocs(schema.docs)
    )
  }

  def withDescriptionJsonSchema[A](
      schema: JsonSchema[A],
      description: String
  ): JsonSchema[A] = {
    def updatedDocs(djs: DocumentedJsonSchema): DocumentedJsonSchema =
      djs match {
        case s: DocumentedRecord => s.copy(description = Some(description))
        case s: DocumentedCoProd => s.copy(description = Some(description))
        case s: Primitive        => s.copy(description = Some(description))
        case s: Array            => s.copy(description = Some(description))
        case s: DocumentedEnum   => s.copy(description = Some(description))
        case s: LazySchema       => LazySchema(updatedDocs(s.value))
        case s: OneOf            => s.copy(description = Some(description))
      }
    new JsonSchema(
      schema.ujsonSchema,
      updatedDocs(schema.docs)
    )
  }

  def orFallbackToJsonSchema[A, B](
      schemaA: JsonSchema[A],
      schemaB: JsonSchema[B]
  ): JsonSchema[Either[A, B]] = {
    new JsonSchema(
      ujsonSchemas
        .orFallbackToJsonSchema(schemaA.ujsonSchema, schemaB.ujsonSchema),
      (schemaA.docs, schemaB.docs) match {
        case (
            OneOf(alternatives1, _, maybeExample1, _),
            OneOf(alternatives2, _, maybeExample2, _)
            ) =>
          OneOf(
            alternatives1 ++ alternatives2,
            example = maybeExample1.orElse(maybeExample2)
          )
        case (OneOf(alternatives, _, maybeExample, _), schema) =>
          OneOf(
            alternatives :+ schema,
            example = maybeExample.orElse(schema.example)
          )
        case (schema, OneOf(alternatives, _, maybeExample, _)) =>
          OneOf(
            schema +: alternatives,
            example = schema.example.orElse(maybeExample)
          )
        case (schema1, schema2) =>
          OneOf(
            List(schema1, schema2),
            example = schema1.example.orElse(schema2.example)
          )
      }
    )
  }

  def stringJsonSchema(format: Option[String]): JsonSchema[String] =
    new JsonSchema(
      ujsonSchemas.stringJsonSchema(format),
      Primitive("string", format)
    )

  lazy val intJsonSchema: JsonSchema[Int] =
    new JsonSchema(
      ujsonSchemas.intJsonSchema,
      Primitive("integer", format = Some("int32"))
    )

  lazy val longJsonSchema: JsonSchema[Long] =
    new JsonSchema(
      ujsonSchemas.longJsonSchema,
      Primitive("integer", format = Some("int64"))
    )

  lazy val bigdecimalJsonSchema: JsonSchema[BigDecimal] =
    new JsonSchema(ujsonSchemas.bigdecimalJsonSchema, Primitive("number"))

  lazy val floatJsonSchema: JsonSchema[Float] =
    new JsonSchema(
      ujsonSchemas.floatJsonSchema,
      Primitive("number", format = Some("float"))
    )

  lazy val doubleJsonSchema: JsonSchema[Double] =
    new JsonSchema(
      ujsonSchemas.doubleJsonSchema,
      Primitive("number", format = Some("double"))
    )

  lazy val booleanJsonSchema: JsonSchema[Boolean] =
    new JsonSchema(ujsonSchemas.booleanJsonSchema, Primitive("boolean"))

  lazy val byteJsonSchema: JsonSchema[Byte] =
    new JsonSchema(ujsonSchemas.byteJsonSchema, Primitive("integer"))

  def arrayJsonSchema[C[X] <: Seq[X], A](
      implicit
      jsonSchema: JsonSchema[A],
      factory: Factory[A, C[A]]
  ): JsonSchema[C[A]] =
    new JsonSchema(
      ujsonSchemas.arrayJsonSchema(jsonSchema.ujsonSchema, factory),
      Array(Left(jsonSchema.docs))
    )

  def mapJsonSchema[A](
      implicit jsonSchema: JsonSchema[A]
  ): JsonSchema[Map[String, A]] =
    new JsonSchema(
      ujsonSchemas.mapJsonSchema(jsonSchema.ujsonSchema),
      DocumentedRecord(
        fields = Nil,
        additionalProperties = Some(jsonSchema.docs)
      )
    )

  /** Convert the internal representation of a JSON schema into the public OpenAPI AST */
  def toSchema(jsonSchema: DocumentedJsonSchema): Schema =
    toSchema(jsonSchema, None, Set.empty)

  private def toSchema(
      documentedCodec: DocumentedJsonSchema,
      coprodBase: Option[(String, DocumentedCoProd)],
      referencedSchemas: Set[String]
  ): Schema = {
    documentedCodec match {
      case record @ DocumentedRecord(_, _, Some(name), _, _, _) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else
          Schema.Reference(
            name,
            Some(
              expandRecordSchema(record, coprodBase, referencedSchemas + name)
            ),
            None
          )
      case record @ DocumentedRecord(_, _, None, _, _, _) =>
        expandRecordSchema(record, coprodBase, referencedSchemas)
      case coprod @ DocumentedCoProd(_, Some(name), _, _, _, _) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else
          Schema.Reference(
            name,
            Some(expandCoproductSchema(coprod, referencedSchemas + name)),
            None
          )
      case coprod @ DocumentedCoProd(_, None, _, _, _, _) =>
        expandCoproductSchema(coprod, referencedSchemas)
      case Primitive(name, format, description, example, title) =>
        Schema.Primitive(name, format, description, example, title)
      case Array(Left(elementType), description, example, title) =>
        Schema.Array(
          Left(toSchema(elementType, coprodBase, referencedSchemas)),
          description,
          example,
          title
        )
      case Array(Right(elementTypes), description, example, title) =>
        Schema.Array(
          Right(
            elementTypes.map(elementType =>
              toSchema(elementType, coprodBase, referencedSchemas)
            )
          ),
          description,
          example,
          title
        )
      case DocumentedEnum(
          elementType,
          values,
          Some(name),
          description,
          example,
          title
          ) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else
          Schema.Reference(
            name,
            Some(
              Schema.Enum(
                toSchema(elementType, coprodBase, referencedSchemas + name),
                values,
                description,
                example,
                title
              )
            ),
            None
          )
      case DocumentedEnum(
          elementType,
          values,
          None,
          description,
          example,
          title
          ) =>
        Schema.Enum(
          toSchema(elementType, coprodBase, referencedSchemas),
          values,
          description,
          example,
          title
        )
      case lzy: LazySchema =>
        toSchema(lzy.value, coprodBase, referencedSchemas)
      case OneOf(alternatives, description, example, title) =>
        val alternativeSchemas = alternatives.map(alternative =>
          toSchema(alternative, coprodBase, referencedSchemas)
        )
        Schema.OneOf(
          EnumeratedAlternatives(alternativeSchemas),
          description,
          example,
          title
        )
    }
  }

  private def expandRecordSchema(
      record: DocumentedJsonSchema.DocumentedRecord,
      coprodBase: Option[(String, DocumentedCoProd)],
      referencedSchemas: Set[String]
  ): Schema = {
    val fieldsSchema = record.fields
      .map(f =>
        Schema.Property(
          f.name,
          toSchema(f.tpe, None, referencedSchemas),
          !f.isOptional,
          f.documentation
        )
      )

    val additionalProperties =
      record.additionalProperties.map(toSchema(_, None, referencedSchemas))

    coprodBase.fold[Schema] {
      Schema.Object(
        fieldsSchema,
        additionalProperties,
        record.description,
        record.example,
        record.title
      )
    } {
      case (tag, coprod) =>
        val discriminatorField =
          Schema.Property(
            coprod.discriminatorName,
            Schema
              .Enum(
                Schema.simpleString,
                List(tag),
                None,
                Some(ujson.Str(tag)),
                None
              ),
            isRequired = true,
            description = None
          )

        coprod.name.fold[Schema] {
          Schema.Object(
            discriminatorField :: fieldsSchema,
            additionalProperties,
            record.description,
            record.example,
            record.title
          )
        } { coproductName =>
          Schema.AllOf(
            schemas = List(
              Schema.Reference(coproductName, None, None),
              Schema.Object(
                discriminatorField :: fieldsSchema,
                additionalProperties,
                None,
                None,
                None
              )
            ),
            record.description,
            record.example,
            record.title
          )
        }
    }
  }

  private def expandCoproductSchema(
      coprod: DocumentedJsonSchema.DocumentedCoProd,
      referencedSchemas: Set[String]
  ): Schema = {
    val alternativesSchemas =
      coprod.alternatives.map {
        case (tag, record) =>
          tag -> toSchema(record, Some(tag -> coprod), referencedSchemas)
      }
    Schema.OneOf(
      DiscriminatedAlternatives(coprod.discriminatorName, alternativesSchemas),
      coprod.description,
      coprod.example,
      coprod.title
    )
  }

}
