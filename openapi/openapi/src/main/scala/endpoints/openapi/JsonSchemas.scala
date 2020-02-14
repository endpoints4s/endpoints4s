package endpoints.openapi

import endpoints.{PartialInvariantFunctor, Tupler, Validated, algebra}
import endpoints.algebra.Documentation
import endpoints.openapi.model.Schema
import endpoints.openapi.model.Schema.{DiscriminatedAlternatives, EnumeratedAlternatives}
import ujson.Value

import scala.annotation.tailrec
import scala.collection.compat._

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces a JSON schema for
  * a given algebraic data type description.
  *
  * @group interpreters
  */
trait JsonSchemas extends algebra.JsonSchemas with TuplesSchemas { openapiJsonSchemas =>

  /**
    * The JSON codecs used to produce some parts of the documentation.
    */
  lazy val ujsonSchemas: endpoints.ujson.JsonSchemas = new endpoints.ujson.JsonSchemas {
    override def defaultDiscriminatorName: String = openapiJsonSchemas.defaultDiscriminatorName
  }

  import DocumentedJsonSchema._

  class JsonSchema[A](val ujsonSchema: ujsonSchemas.JsonSchema[A], val docs: DocumentedJsonSchema)
  class Record[A](override val ujsonSchema: ujsonSchemas.Record[A], override val docs: DocumentedRecord) extends JsonSchema[A](ujsonSchema, docs)
  class Tagged[A](override val ujsonSchema: ujsonSchemas.Tagged[A], override val docs: DocumentedCoProd) extends JsonSchema[A](ujsonSchema, docs)
  class Enum[A](override val ujsonSchema: ujsonSchemas.Enum[A], override val docs: DocumentedEnum) extends JsonSchema[A](ujsonSchema, docs)

  sealed trait DocumentedJsonSchema {
    def example: Option[ujson.Value]
  }

  object DocumentedJsonSchema {

    sealed trait DocumentedRecord extends DocumentedJsonSchema {
      def evaluate: StrictDocumentedRecord
      def fields: List[Field]
      def additionalProperties: Option[DocumentedJsonSchema]
      def name: Option[String]
      def example: Option[ujson.Value]
      def withName(name: String): DocumentedRecord
    }

    object DocumentedRecord {
      def apply(
        fields: List[Field],
        additionalProperties: Option[DocumentedJsonSchema] = None,
        name: Option[String] = None,
        example: Option[ujson.Value] = None
      ): DocumentedRecord = StrictDocumentedRecord(fields, additionalProperties, name, example)
    }
    case class StrictDocumentedRecord(
      fields: List[Field],
      additionalProperties: Option[DocumentedJsonSchema] = None,
      name: Option[String] = None,
      example: Option[ujson.Value] = None
    ) extends DocumentedRecord {
      val evaluate: StrictDocumentedRecord = this
      def withName(name: String): DocumentedRecord = copy(name = Some(name))
    }

    class LazyDocumentedRecord(_record: => StrictDocumentedRecord) extends DocumentedRecord {
      lazy val evaluate: StrictDocumentedRecord = _record

      def fields: List[DocumentedJsonSchema.Field] = evaluate.fields
      def additionalProperties: Option[DocumentedJsonSchema] = evaluate.additionalProperties
      def name: Option[String] = evaluate.name
      def example: Option[Value] = evaluate.example
      def withName(name: String): DocumentedRecord = new LazyDocumentedRecord(evaluate.copy(name = Some(name)))
    }

    case class Field(name: String, tpe: DocumentedJsonSchema, isOptional: Boolean, documentation: Option[String])

    sealed trait DocumentedCoProd extends DocumentedJsonSchema {
      def alternatives: List[(String, DocumentedRecord)]
      def name: Option[String]
      def discriminatorName: String
      def example: Option[ujson.Value]
      def evaluate: StrictDocumentedCoProd
      def withName(name: String): DocumentedCoProd
      def withDiscriminatorName(name: String): DocumentedCoProd
    }

    object DocumentedCoProd {
      def apply(
         alternatives: List[(String, DocumentedRecord)],
         name: Option[String] = None,
         discriminatorName: String = defaultDiscriminatorName,
         example: Option[ujson.Value] = None
       ): DocumentedCoProd = StrictDocumentedCoProd(alternatives, name, discriminatorName, example)
    }

    case class StrictDocumentedCoProd(
      alternatives: List[(String, DocumentedRecord)],
      name: Option[String] = None,
      discriminatorName: String = defaultDiscriminatorName,
      example: Option[ujson.Value] = None
    ) extends DocumentedCoProd {
      val evaluate: StrictDocumentedCoProd = this
      def withName(name: String): DocumentedCoProd = copy(name = Some(name))
      def withAlternatives(alternatives: List[(String, DocumentedRecord)]): DocumentedCoProd = copy(alternatives = alternatives)
      def withDiscriminatorName(name: String): DocumentedCoProd = copy(discriminatorName = name)
    }

    class LazyDocumentedCoProd(_coProd: => StrictDocumentedCoProd) extends DocumentedCoProd {
      lazy val evaluate: StrictDocumentedCoProd = _coProd

      def alternatives: List[(String, DocumentedRecord)] = evaluate.alternatives
      def name: Option[String] = evaluate.name
      def discriminatorName: String = evaluate.discriminatorName
      def example: Option[Value] = evaluate.example

      def withName(name: String): DocumentedCoProd = new LazyDocumentedCoProd(evaluate.copy(name = Some(name)))
      def withDiscriminatorName(name: String): DocumentedCoProd = new LazyDocumentedCoProd(evaluate.copy(discriminatorName = name))
    }

    class LazySchema(_schema: => DocumentedJsonSchema) extends DocumentedJsonSchema {
      lazy val value: DocumentedJsonSchema = _schema
      def example: Option[ujson.Value] = value.example
    }

    case class Primitive(
      name: String,
      format: Option[String] = None,
      example: Option[ujson.Value] = None
    ) extends DocumentedJsonSchema

    /**
      * @param schema `Left(itemSchema)` for a homogeneous array, or `Right(itemSchemas)` for a heterogeneous array (ie, a tuple)
      */
    case class Array(
      schema: Either[DocumentedJsonSchema, List[DocumentedJsonSchema]],
      example: Option[ujson.Value] = None
    ) extends DocumentedJsonSchema

    case class DocumentedEnum(
      elementType: DocumentedJsonSchema,
      values: List[ujson.Value],
      name: Option[String],
      example: Option[ujson.Value] = None
    ) extends DocumentedJsonSchema

    case class OneOf(alternatives: List[DocumentedJsonSchema], example: Option[ujson.Value] = None) extends DocumentedJsonSchema
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
    new Record(schema.ujsonSchema, schema.docs.withName(name))
  def namedTagged[A](schema: Tagged[A], name: String): Tagged[A] =
    new Tagged(schema.ujsonSchema, schema.docs.withName(name))
  def namedEnum[A](schema: Enum[A], name: String): Enum[A] =
    new Enum(schema.ujsonSchema, schema.docs.copy(name = Some(name)))

  def lazyRecord[A](schema: => Record[A], name: String): Record[A] = {
    new Record[A](
      ujsonSchemas.lazyRecord(schema.ujsonSchema, name),
      new LazyDocumentedRecord(namedRecord(schema, name).docs.evaluate)
    )
  }

  def lazyTagged[A](schema: => Tagged[A], name: String): Tagged[A] =
    new Tagged(
      ujsonSchemas.lazyTagged(schema.ujsonSchema, name),
      new LazyDocumentedCoProd(namedTagged(schema, name).docs.evaluate)
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
      tagged.docs.withDiscriminatorName(discriminatorName)
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

  def withExampleJsonSchema[A](schema: JsonSchema[A], example: A): JsonSchema[A] = {
    val exampleJson = schema.ujsonSchema.codec.encode(example)
    def updatedDocs(djs: DocumentedJsonSchema): DocumentedJsonSchema =
      djs match {
        case s: StrictDocumentedRecord => s.copy(example = Some(exampleJson))
        case s: StrictDocumentedCoProd => s.copy(example = Some(exampleJson))
        case s: LazyDocumentedRecord => new LazyDocumentedRecord(s.evaluate.copy(example = Some(exampleJson)))
        case s: LazyDocumentedCoProd => new LazyDocumentedCoProd(s.evaluate.copy(example = Some(exampleJson)))
        case s: LazySchema       => new LazySchema(updatedDocs(s.value))
        case s: Primitive        => s.copy(example = Some(exampleJson))
        case s: Array            => s.copy(example = Some(exampleJson))
        case s: DocumentedEnum   => s.copy(example = Some(exampleJson))
        case s: OneOf            => s.copy(example = Some(exampleJson))
      }

    new JsonSchema(
      schema.ujsonSchema,
      updatedDocs(schema.docs)
    )
  }

  def orFallbackToJsonSchema[A, B](schemaA: JsonSchema[A], schemaB: JsonSchema[B]): JsonSchema[Either[A, B]] = {
    new JsonSchema(
      ujsonSchemas.orFallbackToJsonSchema(schemaA.ujsonSchema, schemaB.ujsonSchema),
      (schemaA.docs, schemaB.docs) match {
        case (OneOf(alternatives1, maybeExample1), OneOf(alternatives2, maybeExample2)) =>
          OneOf(alternatives1 ++ alternatives2, maybeExample1.orElse(maybeExample2))
        case (OneOf(alternatives, maybeExample), schema) =>
          OneOf(alternatives :+ schema, maybeExample.orElse(schema.example))
        case (schema, OneOf(alternatives, maybeExample)) =>
          OneOf(schema +: alternatives, schema.example.orElse(maybeExample))
        case (schema1, schema2) =>
          OneOf(List(schema1, schema2), schema1.example.orElse(schema2.example))
      }
    )
  }

  def stringJsonSchema(format: Option[String]): JsonSchema[String] =
    new JsonSchema(ujsonSchemas.stringJsonSchema(format), Primitive("string", format))

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

  /** Convert the internal representation of a JSON schema into the public OpenAPI AST */
  def toSchema(jsonSchema: DocumentedJsonSchema): Schema =
    toSchema(jsonSchema, None, Set.empty)

  private def toSchema(documentedCodec: DocumentedJsonSchema, coprodBase: Option[(String, DocumentedCoProd)], referencedSchemas: Set[String]): Schema = {
    @tailrec
    def inner(innerSchema: DocumentedJsonSchema): Schema =
      innerSchema match {
        case record @ StrictDocumentedRecord(_, _, Some(name), _) =>
          if (referencedSchemas(name)) Schema.Reference(name, None, None)
          else Schema.Reference(name, Some(expandRecordSchema(record, coprodBase, referencedSchemas + name)), None)
        case record @ StrictDocumentedRecord(_, _, None, _) =>
          expandRecordSchema(record, coprodBase, referencedSchemas)
        case record: LazyDocumentedRecord => inner(record.evaluate)
        case coprod @ StrictDocumentedCoProd(_, Some(name), _, _) =>
          if (referencedSchemas(name)) Schema.Reference(name, None, None)
          else Schema.Reference(name, Some(expandCoproductSchema(coprod, referencedSchemas + name)), None)
        case coprod @ StrictDocumentedCoProd(_, None, _, _) =>
          expandCoproductSchema(coprod, referencedSchemas)
        case coprod: LazyDocumentedCoProd => inner(coprod.evaluate)
        case lzy: LazySchema => inner(lzy.value)
        case Primitive(name, format, example) =>
          Schema.Primitive(name, format, None, example)
        case Array(Left(elementType), example) =>
          Schema.Array(Left(toSchema(elementType, coprodBase, referencedSchemas)), None, example)
        case Array(Right(elementTypes), example) =>
          Schema.Array(Right(elementTypes.map(elementType => toSchema(elementType, coprodBase, referencedSchemas))), None, example)
        case DocumentedEnum(elementType, values, Some(name), example) =>
          if (referencedSchemas(name)) Schema.Reference(name, None, None)
          else Schema.Reference(name, Some(Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas + name), values, None, example)), None)
        case DocumentedEnum(elementType, values, None, example) =>
          Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas), values, None, example)

        case OneOf(alternatives, example) =>
          val alternativeSchemas = alternatives.map(alternative => toSchema(alternative, coprodBase, referencedSchemas))
          Schema.OneOf(EnumeratedAlternatives(alternativeSchemas), None, example)
      }

    inner(documentedCodec)
  }

  private def expandRecordSchema(record: DocumentedJsonSchema.DocumentedRecord, coprodBase: Option[(String, DocumentedCoProd)], referencedSchemas: Set[String]): Schema = {
    val fieldsSchema = record.fields
      .map(f => Schema.Property(f.name, toSchema(f.tpe, None, referencedSchemas), !f.isOptional, f.documentation))

    val additionalProperties = record.additionalProperties.map(toSchema(_, None, referencedSchemas))

    coprodBase.fold[Schema] {
      Schema.Object(fieldsSchema, additionalProperties, None, record.example)
    } { case (tag, coprod) =>
      val discriminatorField =
        Schema.Property(
          coprod.discriminatorName,
          Schema.Enum(Schema.simpleString, List(tag), None, Some(ujson.Str(tag))),
          isRequired = true,
          description = None
        )

      coprod.name.fold[Schema] {
        Schema.Object(discriminatorField :: fieldsSchema, additionalProperties, None, record.example)
      } { coproductName =>
        Schema.AllOf(
          schemas = List(
            Schema.Reference(coproductName, None, None),
            Schema.Object(discriminatorField :: fieldsSchema, additionalProperties, None, None)
          ),
          description = None,
          record.example
        )
      }
    }
  }

  private def expandCoproductSchema(coprod: DocumentedJsonSchema.DocumentedCoProd, referencedSchemas: Set[String]): Schema = {
    val alternativesSchemas =
      coprod.alternatives.map { case (tag, record) => tag -> toSchema(record, Some(tag -> coprod), referencedSchemas) }
    Schema.OneOf(DiscriminatedAlternatives(coprod.discriminatorName, alternativesSchemas), None, coprod.example)
  }

}
