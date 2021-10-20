package endpoints4s.openapi

import endpoints4s.{
  Hashing,
  NumericConstraints,
  PartialInvariantFunctor,
  Tupler,
  Validated,
  algebra
}
import endpoints4s.algebra.Documentation
import endpoints4s.openapi.model.Schema
import endpoints4s.openapi.model.Schema.{DiscriminatedAlternatives, EnumeratedAlternatives}

import scala.collection.compat._

/** An interpreter for [[endpoints4s.algebra.JsonSchemas]] that produces a JSON schema for a given
  * algebraic data type description.
  *
  * The encoding of the schemas of sealed traits (obtained with the operation `orElse` or via
  * generic derivation) can be configured by overriding [[JsonSchemas.coproductEncoding]].
  *
  * @group interpreters
  */
trait JsonSchemas extends algebra.JsonSchemas with TuplesSchemas {
  openapiJsonSchemas =>

  /** The JSON codecs used to produce some parts of the documentation.
    */
  final lazy val ujsonSchemas: endpoints4s.ujson.JsonSchemas =
    new endpoints4s.ujson.JsonSchemas {

      override def defaultDiscriminatorName: String =
        openapiJsonSchemas.defaultDiscriminatorName

      override def encodersSkipDefaultValues: Boolean = true
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

    sealed abstract class DocumentedRecord extends DocumentedJsonSchema {
      def fields: List[Field]
      def additionalProperties: Option[DocumentedJsonSchema]
      def name: Option[String]

      def withName(name: String): DocumentedRecord
      def withExample(example: => ujson.Value): DocumentedRecord
      def withTitle(title: String): DocumentedRecord
      def withDescription(description: String): DocumentedRecord
    }
    object DocumentedRecord {

      case class Immediate(
          fields: List[Field],
          additionalProperties: Option[DocumentedJsonSchema] = None,
          name: Option[String] = None,
          description: Option[String] = None,
          example: Option[ujson.Value] = None,
          title: Option[String] = None
      ) extends DocumentedRecord {
        def withName(name: String): DocumentedRecord =
          copy(name = Some(name))
        def withExample(example: => ujson.Value): DocumentedRecord =
          copy(example = Some(example))
        def withTitle(title: String): DocumentedRecord =
          copy(title = Some(title))
        def withDescription(description: String): DocumentedRecord =
          copy(description = Some(description))
      }

      class Lazy(n: String, docs: => DocumentedRecord) extends DocumentedRecord {
        lazy val evaluatedDocs = docs
        def fields: List[Field] = evaluatedDocs.fields
        def additionalProperties: Option[DocumentedJsonSchema] =
          evaluatedDocs.additionalProperties
        def name: Option[String] = Some(n)
        def description: Option[String] = evaluatedDocs.description
        def example: Option[ujson.Value] = evaluatedDocs.example
        def title: Option[String] = evaluatedDocs.title

        def withName(n: String): DocumentedRecord =
          new Lazy(n, docs)
        def withExample(e: => ujson.Value): DocumentedRecord =
          new Lazy(n, this) {
            override def example: Option[ujson.Value] = Some(e)
          }
        def withTitle(t: String): DocumentedRecord =
          new Lazy(n, this) {
            override def title: Option[String] = Some(t)
          }
        def withDescription(d: String): DocumentedRecord =
          new Lazy(n, this) {
            override def description: Option[String] = Some(d)
          }
      }

      def apply(
          fields: List[Field],
          additionalProperties: Option[DocumentedJsonSchema] = None,
          name: Option[String] = None,
          description: Option[String] = None,
          example: Option[ujson.Value] = None,
          title: Option[String] = None
      ): DocumentedRecord = {
        Immediate(fields, additionalProperties, name, description, example, title)
      }

    }

    @deprecatedInheritance
    class Field private (
        val name: String,
        val tpe: DocumentedJsonSchema,
        val isOptional: Boolean,
        val default: Option[ujson.Value],
        val documentation: Option[String]
    ) extends Serializable
        with Product
        with Equals {

      @deprecated("`Field` is no longer a `case class` and won't implement `Equals`", "3.1.0")
      override def canEqual(other: Any): Boolean = other.isInstanceOf[Field]
      @deprecated("`Field` is no longer a `case class` and won't implement `Product`", "3.1.0")
      override def productArity: Int = 5
      @deprecated("`Field` is no longer a `case class` and won't implement `Product`", "3.1.0")
      override def productElement(idx: Int): Any = idx match {
        case 0 => name
        case 1 => tpe
        case 2 => isOptional
        case 3 => default
        case 4 => documentation
        case _ => throw new IndexOutOfBoundsException(idx.toString)
      }
      override def toString: String = s"Field($name,$tpe,$isOptional,$default,$documentation)"
      override def hashCode: Int = Hashing.hash(name, tpe, isOptional, default, documentation)
      override def equals(other: Any): Boolean = other match {
        case field: Field =>
          name == field.name && tpe == field.tpe && isOptional == field.isOptional &&
            default == field.default && documentation == field.documentation
        case _ => false
      }

      @deprecated("Use the Field apply method instead", "3.1.0")
      def this(
          name: String,
          tpe: DocumentedJsonSchema,
          isOptional: Boolean,
          documentation: Option[String]
      ) = this(name, tpe, isOptional, None, documentation)

      @deprecated("Use `withName`, `withTpe`, etc. instead of `copy`", "3.1.0")
      def copy(
          name: String = name,
          tpe: DocumentedJsonSchema = tpe,
          isOptional: Boolean = isOptional,
          documentation: Option[String] = documentation
      ): Field =
        new Field(name, tpe, isOptional, default, documentation)

      def withName(name: String): Field =
        new Field(name, tpe, isOptional, default, documentation)

      def withTpe(tpe: DocumentedJsonSchema): Field =
        new Field(name, tpe, isOptional, default, documentation)

      def withIsOptional(isOptional: Boolean): Field =
        new Field(name, tpe, isOptional, default, documentation)

      def withDocumentation(documentation: Option[String]): Field =
        new Field(name, tpe, isOptional, default, documentation)
    }

    object Field
        extends runtime.AbstractFunction4[String, DocumentedJsonSchema, Boolean, Option[
          String
        ], Field] {

      @deprecated(
        "The Field apply method now takes an additional parameter 'default'",
        "3.1.0"
      )
      def apply(
          name: String,
          tpe: DocumentedJsonSchema,
          isOptional: Boolean,
          documentation: Option[String]
      ): Field = new Field(name, tpe, isOptional, None, documentation)

      def apply(
          name: String,
          tpe: DocumentedJsonSchema,
          isOptional: Boolean,
          defaultValue: Option[ujson.Value],
          documentation: Option[String]
      ): Field = new Field(name, tpe, isOptional, defaultValue, documentation)

      @deprecated("Use field extractors instead of unapply", "3.1.0")
      def unapply(field: Field): Option[(String, DocumentedJsonSchema, Boolean, Option[String])] =
        Some(
          (
            field.name,
            field.tpe,
            field.isOptional,
            field.documentation
          )
        )
    }

    sealed abstract class DocumentedCoProd extends DocumentedJsonSchema {
      def alternatives: List[(String, DocumentedRecord)]
      def name: Option[String]
      def discriminatorName: String

      def withName(name: String): DocumentedCoProd
      def withExample(example: => ujson.Value): DocumentedCoProd
      def withTitle(title: String): DocumentedCoProd
      def withDescription(description: String): DocumentedCoProd
      def withDiscriminatorName(discriminatorName: String): DocumentedCoProd
    }

    object DocumentedCoProd {
      case class Immediate(
          alternatives: List[(String, DocumentedRecord)],
          name: Option[String] = None,
          discriminatorName: String = defaultDiscriminatorName,
          description: Option[String] = None,
          example: Option[ujson.Value] = None,
          title: Option[String] = None
      ) extends DocumentedCoProd {
        def withName(name: String): DocumentedCoProd =
          copy(name = Some(name))
        def withExample(example: => ujson.Value): DocumentedCoProd =
          copy(example = Some(example))
        def withTitle(title: String): DocumentedCoProd =
          copy(title = Some(title))
        def withDescription(description: String): DocumentedCoProd =
          copy(description = Some(description))
        def withDiscriminatorName(discriminatorName: String): DocumentedCoProd =
          copy(discriminatorName = discriminatorName)
      }

      class Lazy(n: String, docs: => DocumentedCoProd) extends DocumentedCoProd {
        lazy val evaluatedDocs = docs
        def alternatives: List[(String, DocumentedRecord)] = evaluatedDocs.alternatives
        def name: Option[String] = Some(n)
        def discriminatorName: String = evaluatedDocs.discriminatorName
        def description: Option[String] = evaluatedDocs.description
        def example: Option[ujson.Value] = evaluatedDocs.example
        def title: Option[String] = evaluatedDocs.title

        def withName(n: String): DocumentedCoProd =
          new Lazy(n, docs)
        def withExample(e: => ujson.Value): DocumentedCoProd =
          new Lazy(n, this) {
            override def example: Option[ujson.Value] = Some(e)
          }
        def withTitle(t: String): DocumentedCoProd =
          new Lazy(n, this) {
            override def title: Option[String] = Some(t)
          }
        def withDescription(d: String): DocumentedCoProd =
          new Lazy(n, this) {
            override def description: Option[String] = Some(d)
          }
        def withDiscriminatorName(d: String): DocumentedCoProd =
          new Lazy(n, this) {
            override def discriminatorName: String = d
          }
      }

      def apply(
          alternatives: List[(String, DocumentedRecord)],
          name: Option[String] = None,
          discriminatorName: String = defaultDiscriminatorName,
          description: Option[String] = None,
          example: Option[ujson.Value] = None,
          title: Option[String] = None
      ): DocumentedCoProd =
        Immediate(alternatives, name, discriminatorName, description, example, title)
    }

    case class Primitive(
        name: String,
        format: Option[String] = None,
        description: Option[String] = None,
        example: Option[ujson.Value] = None,
        title: Option[String] = None,
        minimum: Option[Double] = None,
        exclusiveMinimum: Option[Boolean] = None,
        maximum: Option[Double] = None,
        exclusiveMaximum: Option[Boolean] = None,
        multipleOf: Option[Double] = None
    ) extends DocumentedJsonSchema

    /** @param schema
      *   `Left(itemSchema)` for a homogeneous array, or `Right(itemSchemas)` for a heterogeneous
      *   array (ie, a tuple)
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
      def name: String
      def value: DocumentedJsonSchema
    }

    object LazySchema {

      def apply(n: String, s: => DocumentedJsonSchema): LazySchema =
        new LazySchema {
          override val name: String = n
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

  implicit def jsonSchemaPartialInvFunctor: PartialInvariantFunctor[JsonSchema] =
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
    new Record(schema.ujsonSchema, schema.docs.withName(name))

  def namedTagged[A](schema: Tagged[A], name: String): Tagged[A] =
    new Tagged(schema.ujsonSchema, schema.docs.withName(name))

  def namedEnum[A](schema: Enum[A], name: String): Enum[A] =
    new Enum(schema.ujsonSchema, schema.docs.copy(name = Some(name)))

  override def lazySchema[A](name: String)(schema: => JsonSchema[A]): JsonSchema[A] =
    new JsonSchema(
      ujsonSchemas.lazySchema(name)(schema.ujsonSchema),
      LazySchema(name, schema.docs)
    )
  def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A] =
    lazySchema(name)(schema)
  def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A] =
    lazySchema(name)(schema)

  override def lazyRecord[A](name: String)(schema: => Record[A]): Record[A] =
    new Record[A](
      ujsonSchemas.lazyRecord(name)(schema.ujsonSchema),
      new DocumentedRecord.Lazy(name, schema.docs)
    )

  override def lazyTagged[A](name: String)(schema: => Tagged[A]): Tagged[A] =
    new Tagged[A](
      ujsonSchemas.lazyTagged(name)(schema.ujsonSchema),
      new DocumentedCoProd.Lazy(name, schema.docs)
    )

  def emptyRecord: Record[Unit] =
    new Record(ujsonSchemas.emptyRecord, DocumentedRecord(Nil))

  def field[A](name: String, docs: Documentation)(implicit
      tpe: JsonSchema[A]
  ): Record[A] =
    new Record(
      ujsonSchemas.field(name, docs)(tpe.ujsonSchema),
      DocumentedRecord(Field(name, tpe.docs, isOptional = false, defaultValue = None, docs) :: Nil)
    )

  def optField[A](name: String, docs: Documentation)(implicit
      tpe: JsonSchema[A]
  ): Record[Option[A]] =
    new Record(
      ujsonSchemas.optField(name, docs)(tpe.ujsonSchema),
      DocumentedRecord(Field(name, tpe.docs, isOptional = true, defaultValue = None, docs) :: Nil)
    )

  override def optFieldWithDefault[A](
      name: String,
      defaultValue: A,
      docs: Option[String] = None
  )(implicit
      tpe: JsonSchema[A]
  ): Record[A] = {
    val defValue = Some(tpe.ujsonSchema.encoder.encode(defaultValue))
    new Record(
      ujsonSchemas.optFieldWithDefault(name, defaultValue, docs)(tpe.ujsonSchema),
      DocumentedRecord(Field(name, tpe.docs, isOptional = true, defValue, docs) :: Nil)
    )
  }

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
      tagged.docs.withDiscriminatorName(discriminatorName)
    )

  def choiceTagged[A, B](
      taggedA: Tagged[A],
      taggedB: Tagged[B]
  ): Tagged[Either[A, B]] =
    new Tagged(
      ujsonSchemas.choiceTagged(taggedA.ujsonSchema, taggedB.ujsonSchema),
      DocumentedCoProd(taggedA.docs.alternatives ++ taggedB.docs.alternatives)
    )

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B])(implicit
      t: Tupler[A, B]
  ): Record[t.Out] =
    new Record(
      ujsonSchemas.zipRecords(recordA.ujsonSchema, recordB.ujsonSchema),
      DocumentedRecord(recordA.docs.fields ++ recordB.docs.fields)
    )

  def withExampleRecord[A](
      record: Record[A],
      example: A
  ): Record[A] =
    new Record[A](
      record.ujsonSchema,
      record.docs.withExample(record.ujsonSchema.codec.encode(example))
    )

  def withExampleTagged[A](
      tagged: Tagged[A],
      example: A
  ): Tagged[A] =
    new Tagged[A](
      tagged.ujsonSchema,
      tagged.docs.withExample(tagged.ujsonSchema.codec.encode(example))
    )

  def withExampleEnum[A](
      enumeration: Enum[A],
      example: A
  ): Enum[A] = {
    val exampleJson = enumeration.ujsonSchema.codec.encode(example)
    new Enum[A](
      enumeration.ujsonSchema,
      enumeration.docs.copy(example = Some(exampleJson))
    )
  }

  def withExampleJsonSchema[A](
      schema: JsonSchema[A],
      example: A
  ): JsonSchema[A] = {
    lazy val exampleJson = schema.ujsonSchema.codec.encode(example)
    def updatedDocs(djs: DocumentedJsonSchema): DocumentedJsonSchema =
      djs match {
        case s: DocumentedRecord => s.withExample(exampleJson)
        case s: DocumentedCoProd => s.withExample(exampleJson)
        case s: Primitive        => s.copy(example = Some(exampleJson))
        case s: Array            => s.copy(example = Some(exampleJson))
        case s: DocumentedEnum   => s.copy(example = Some(exampleJson))
        case s: LazySchema       => LazySchema(s.name, updatedDocs(s.value))
        case s: OneOf            => s.copy(example = Some(exampleJson))
      }
    new JsonSchema(
      schema.ujsonSchema,
      updatedDocs(schema.docs)
    )
  }

  def withTitleRecord[A](
      record: Record[A],
      title: String
  ): Record[A] =
    new Record[A](
      record.ujsonSchema,
      record.docs.withTitle(title)
    )

  def withTitleTagged[A](
      tagged: Tagged[A],
      title: String
  ): Tagged[A] =
    new Tagged[A](
      tagged.ujsonSchema,
      tagged.docs.withTitle(title)
    )

  def withTitleEnum[A](
      enumeration: Enum[A],
      title: String
  ): Enum[A] =
    new Enum[A](
      enumeration.ujsonSchema,
      enumeration.docs.copy(title = Some(title))
    )

  def withTitleJsonSchema[A](
      schema: JsonSchema[A],
      title: String
  ): JsonSchema[A] = {
    def updatedDocs(djs: DocumentedJsonSchema): DocumentedJsonSchema =
      djs match {
        case s: DocumentedRecord => s.withTitle(title)
        case s: DocumentedCoProd => s.withTitle(title)
        case s: Primitive        => s.copy(title = Some(title))
        case s: Array            => s.copy(title = Some(title))
        case s: DocumentedEnum   => s.copy(title = Some(title))
        case s: LazySchema       => LazySchema(s.name, updatedDocs(s.value))
        case s: OneOf            => s.copy(title = Some(title))
      }
    new JsonSchema(
      schema.ujsonSchema,
      updatedDocs(schema.docs)
    )
  }

  def withDescriptionRecord[A](
      record: Record[A],
      description: String
  ): Record[A] =
    new Record[A](
      record.ujsonSchema,
      record.docs.withDescription(description)
    )

  def withDescriptionTagged[A](
      tagged: Tagged[A],
      description: String
  ): Tagged[A] =
    new Tagged[A](
      tagged.ujsonSchema,
      tagged.docs.withDescription(description)
    )

  def withDescriptionEnum[A](
      enumeration: Enum[A],
      description: String
  ): Enum[A] =
    new Enum[A](
      enumeration.ujsonSchema,
      enumeration.docs.copy(description = Some(description))
    )

  def withDescriptionJsonSchema[A](
      schema: JsonSchema[A],
      description: String
  ): JsonSchema[A] = {
    def updatedDocs(djs: DocumentedJsonSchema): DocumentedJsonSchema =
      djs match {
        case s: DocumentedRecord => s.withDescription(description)
        case s: DocumentedCoProd => s.withDescription(description)
        case s: Primitive        => s.copy(description = Some(description))
        case s: Array            => s.copy(description = Some(description))
        case s: DocumentedEnum   => s.copy(description = Some(description))
        case s: LazySchema       => LazySchema(s.name, updatedDocs(s.value))
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

  implicit lazy val intJsonSchema: JsonSchema[Int] =
    intWithConstraintsJsonSchema(NumericConstraints[Int])

  implicit lazy val longJsonSchema: JsonSchema[Long] =
    longWithConstraintsJsonSchema(NumericConstraints[Long])

  implicit lazy val bigdecimalJsonSchema: JsonSchema[BigDecimal] =
    bigdecimalWithConstraintsJsonSchema(NumericConstraints[BigDecimal])

  implicit lazy val floatJsonSchema: JsonSchema[Float] =
    floatWithConstraintsJsonSchema(NumericConstraints[Float])

  implicit lazy val doubleJsonSchema: JsonSchema[Double] =
    doubleWithConstraintsJsonSchema(NumericConstraints[Double])

  private def toDouble[A](opt: Option[A])(implicit num: Numeric[A]) =
    opt.map(a => num.toDouble(a))

  override def intWithConstraintsJsonSchema(constraints: NumericConstraints[Int]): JsonSchema[Int] =
    new JsonSchema(
      ujsonSchemas.intJsonSchema,
      Primitive(
        "integer",
        format = Some("int32"),
        minimum = toDouble(constraints.minimum),
        exclusiveMinimum = constraints.exclusiveMinimum,
        maximum = toDouble(constraints.maximum),
        exclusiveMaximum = constraints.exclusiveMaximum,
        multipleOf = toDouble(constraints.multipleOf)
      )
    )

  override def longWithConstraintsJsonSchema(
      constraints: NumericConstraints[Long]
  ): JsonSchema[Long] =
    new JsonSchema(
      ujsonSchemas.longJsonSchema,
      Primitive(
        "integer",
        format = Some("int64"),
        minimum = toDouble(constraints.minimum),
        exclusiveMinimum = constraints.exclusiveMinimum,
        maximum = toDouble(constraints.maximum),
        exclusiveMaximum = constraints.exclusiveMaximum,
        multipleOf = toDouble(constraints.multipleOf)
      )
    )

  override def bigdecimalWithConstraintsJsonSchema(
      constraints: NumericConstraints[BigDecimal]
  ): JsonSchema[BigDecimal] =
    new JsonSchema(
      ujsonSchemas.bigdecimalJsonSchema,
      Primitive(
        "number",
        minimum = toDouble(constraints.minimum),
        exclusiveMinimum = constraints.exclusiveMinimum,
        maximum = toDouble(constraints.maximum),
        exclusiveMaximum = constraints.exclusiveMaximum,
        multipleOf = toDouble(constraints.multipleOf)
      )
    )

  override def floatWithConstraintsJsonSchema(
      constraints: NumericConstraints[Float]
  ): JsonSchema[Float] =
    new JsonSchema(
      ujsonSchemas.floatJsonSchema,
      Primitive(
        "number",
        format = Some("float"),
        minimum = toDouble(constraints.minimum),
        exclusiveMinimum = constraints.exclusiveMinimum,
        maximum = toDouble(constraints.maximum),
        exclusiveMaximum = constraints.exclusiveMaximum,
        multipleOf = toDouble(constraints.multipleOf)
      )
    )

  override def doubleWithConstraintsJsonSchema(
      constraints: NumericConstraints[Double]
  ): JsonSchema[Double] =
    new JsonSchema(
      ujsonSchemas.doubleJsonSchema,
      Primitive(
        "number",
        format = Some("double"),
        minimum = toDouble(constraints.minimum),
        exclusiveMinimum = constraints.exclusiveMinimum,
        maximum = toDouble(constraints.maximum),
        exclusiveMaximum = constraints.exclusiveMaximum,
        multipleOf = toDouble(constraints.multipleOf)
      )
    )

  lazy val booleanJsonSchema: JsonSchema[Boolean] =
    new JsonSchema(ujsonSchemas.booleanJsonSchema, Primitive("boolean"))

  lazy val byteJsonSchema: JsonSchema[Byte] =
    new JsonSchema(ujsonSchemas.byteJsonSchema, Primitive("integer"))

  def arrayJsonSchema[C[X] <: Iterable[X], A](implicit
      jsonSchema: JsonSchema[A],
      factory: Factory[A, C[A]]
  ): JsonSchema[C[A]] =
    new JsonSchema(
      ujsonSchemas.arrayJsonSchema(jsonSchema.ujsonSchema, factory),
      Array(Left(jsonSchema.docs))
    )

  def mapJsonSchema[A](implicit
      jsonSchema: JsonSchema[A]
  ): JsonSchema[Map[String, A]] =
    new JsonSchema(
      ujsonSchemas.mapJsonSchema(jsonSchema.ujsonSchema),
      DocumentedRecord(
        fields = Nil,
        additionalProperties = Some(jsonSchema.docs)
      )
    )

  sealed trait CoproductEncoding

  /** This object contains the options for how to encode coproduct JSON schemas.
    *
    * The following Scala coproduct is the candidate example. Each encoding option includes the
    * schema that it would generate for that example.
    *
    * {{{
    * sealed trait Pet
    * case class Cat(name: String) extends Pet
    * case class Lizard(lovesRocks: Boolean) extends Pet
    * }}}
    */
  object CoproductEncoding {

    /** Strategy defining the base type schema in terms of `oneOf` and the variant schemas. The
      * variants themselves don't refer to the base type, but they do include the discriminator
      * field.
      *
      *   - simpler looking schemas in Swagger UI
      *   - some OpenAPI clients don't handle `oneOf` properly
      *
      * Using the `Pet` example above, this strategy yields the following:
      *
      * {{{
      * "schemas": {
      *   "Pet": {
      *     "oneOf": [
      *       { "\$ref": "#/components/schemas/Cat" },
      *       { "\$ref": "#/components/schemas/Lizard" }
      *     ],
      *     "discriminator": {
      *       "propertyName": "type",
      *       "mapping": {
      *         "Cat": "#/components/schemas/Cat",
      *         "Lizard": "#/components/schemas/Lizard"
      *       }
      *     }
      *   },
      *
      *   "Cat": {
      *     "type": "object",
      *     "properties": {
      *       "type": {
      *         "type": "string",
      *         "enum": [ "Cat" ]
      *       },
      *       "name": {
      *         "type": "string"
      *       }
      *     },
      *     "required": [
      *       "type",
      *       "name"
      *     ]
      *   },
      *
      *   "Lizard": {
      *     "type": "object",
      *     "properties": {
      *       "type": {
      *         "type": "string",
      *         "enum": [ "Lizard" ]
      *       },
      *       "lovesRocks": {
      *         "type": "boolean"
      *       }
      *     },
      *     "required": [
      *       "type",
      *       "lovesRocks"
      *     ]
      *   }
      * }
      * }}}
      */
    case object OneOf extends CoproductEncoding

    /** Strategy that extends [[OneOf]] so that each variant also refers back to the base type
      * schema using `allOf`. This approach is sometimes referred to in OpenAPI 3 as a way to model
      * polymorphism.
      *
      *   - compatible with OpenAPI clients that don't handle `oneOf` properly
      *   - more complex schemas in Swagger UI
      *
      * Using the `Pet` example above, this strategy yields the following:
      *
      * {{{
      * "schemas": {
      *   "Pet": {
      *     "oneOf": [
      *       { "\$ref": "#/components/schemas/Cat" },
      *       { "\$ref": "#/components/schemas/Lizard" }
      *     ],
      *     "discriminator": {
      *       "propertyName": "type",
      *       "mapping": {
      *         "Cat": "#/components/schemas/Cat",
      *         "Lizard": "#/components/schemas/Lizard"
      *       }
      *     }
      *   },
      *
      *   "Cat": {
      *     "allOf": [
      *       { "\$ref": "#/components/schemas/Pet" },
      *       {
      *         "type": "object",
      *         "properties": {
      *           "type": {
      *             "type": "string",
      *             "enum": [ "Cat" ]
      *           },
      *           "name": {
      *             "type": "string"
      *           }
      *         },
      *         "required": [
      *           "type",
      *           "name"
      *         ]
      *       }
      *     ]
      *   },
      *
      *   "Lizard": {
      *     "allOf": [
      *       { "\$ref": "#/components/schemas/Pet" },
      *       {
      *         "type": "object",
      *         "properties": {
      *           "type": {
      *             "type": "string",
      *             "enum": [ "Lizard" ]
      *           },
      *           "lovesRocks": {
      *             "type": "boolean"
      *           }
      *         },
      *         "required": [
      *           "type",
      *           "lovesRocks"
      *         ]
      *       }
      *     ]
      *   }
      * }
      * }}}
      */
    case object OneOfWithBaseRef extends CoproductEncoding
  }

  /** Override this method to customize the strategy used to encode the JSON schema of coproducts.
    * By default, it uses [[CoproductEncoding.OneOf]].
    *
    * @see
    *   [[CoproductEncoding]]
    */
  def coproductEncoding: CoproductEncoding = CoproductEncoding.OneOf

  /** Convert the internal representation of a JSON schema into the public OpenAPI AST */
  def toSchema(jsonSchema: DocumentedJsonSchema): Schema =
    toSchema(jsonSchema, None, Set.empty)

  private def toSchema(
      documentedCodec: DocumentedJsonSchema,
      coprodBase: Option[(String, DocumentedCoProd)],
      referencedSchemas: Set[String]
  ): Schema = {
    documentedCodec match {
      case record: DocumentedRecord =>
        record.name match {
          case Some(name) =>
            if (referencedSchemas(name)) Schema.Reference(name, None, None)
            else
              Schema.Reference(
                name,
                Some(
                  expandRecordSchema(record, coprodBase, referencedSchemas + name)
                ),
                None
              )
          case None =>
            expandRecordSchema(record, coprodBase, referencedSchemas)
        }
      case coprod: DocumentedCoProd =>
        coprod.name match {
          case Some(name) =>
            if (referencedSchemas(name)) Schema.Reference(name, None, None)
            else
              Schema.Reference(
                name,
                Some(expandCoproductSchema(coprod, referencedSchemas + name)),
                None
              )
          case None =>
            expandCoproductSchema(coprod, referencedSchemas)
        }
      case Primitive(
            name,
            format,
            description,
            example,
            title,
            min,
            exclusiveMin,
            max,
            exclusiveMax,
            multipleOf
          ) =>
        Schema
          .Primitive(name, format, description, example, title)
          .withMinimum(min)
          .withExclusiveMinimum(exclusiveMin)
          .withMaximum(max)
          .withExclusiveMaximum(exclusiveMax)
          .withMultipleOf(multipleOf)
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
            elementTypes.map(elementType => toSchema(elementType, coprodBase, referencedSchemas))
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
        if (referencedSchemas(lzy.name)) Schema.Reference(lzy.name, None, None)
        else
          Schema.Reference(
            lzy.name,
            Some(toSchema(lzy.value, coprodBase, referencedSchemas + lzy.name)),
            None
          )
      case OneOf(alternatives, description, example, title) =>
        val alternativeSchemas =
          alternatives.map(alternative => toSchema(alternative, coprodBase, referencedSchemas))
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
          f.default,
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
    } { case (tag, coprod) =>
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
          defaultValue = None,
          description = None
        )

      (coprod.name, coproductEncoding) match {
        case (Some(coproductName), CoproductEncoding.OneOfWithBaseRef) =>
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

        case _ =>
          Schema.Object(
            discriminatorField :: fieldsSchema,
            additionalProperties,
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
      coprod.alternatives.map { case (tag, record) =>
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
