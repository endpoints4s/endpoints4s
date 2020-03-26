package endpoints.algebra

import java.util.UUID

import endpoints.{
  PartialInvariantFunctor,
  PartialInvariantFunctorSyntax,
  Tupler,
  Validated
}

import scala.collection.compat._
import scala.util.control.Exception

/**
  * An algebra interface for describing algebraic data types. Such descriptions
  * can be interpreted to produce a JSON schema of the data type, a JSON encoder,
  * a JSON decoder, etc.
  *
  * A description contains the fields of a case class and their type, and the
  * constructor names of a sealed trait.
  *
  * For instance, consider the following record type:
  *
  * {{{
  *   case class User(name: String, age: Int)
  * }}}
  *
  * Its description is the following:
  *
  * {{{
  *   object User {
  *     implicit val schema: JsonSchema[User] = (
  *       field[String]("name") zip
  *       field[Int]("age")
  *     ).xmap((User.apply _).tupled)(Function.unlift(User.unapply))
  *   }
  * }}}
  *
  * The description says that the record type has two fields, the first one has type `String` and is
  * named “name”, and the second one has type `Int` and name “age”.
  *
  * To describe sum types you have to explicitly “tag” each alternative:
  *
  * {{{
  *   sealed trait Shape
  *   case class Circle(radius: Double) extends Shape
  *   case class Rectangle(width: Double, height: Double) extends Shape
  *
  *   object Shape {
  *     implicit val schema: JsonSchema[Shape] = {
  *       val circleSchema = field[Double]("radius").xmap(Circle)(Function.unlift(Circle.unapply))
  *       val rectangleSchema = (
  *         field[Double]("width") zip
  *         field[Double]("height")
  *       ).xmap((Rectangle.apply _).tupled)(Function.unlift(Rectangle.unapply))
  *       (circleSchema.tagged("Circle") orElse rectangleSchema.tagged("Rectangle"))
  *         .xmap[Shape] {
  *           case Left(circle) => circle
  *           case Right(rect)  => rect
  *         } {
  *           case c: Circle    => Left(c)
  *           case r: Rectangle => Right(r)
  *         }
  *     }
  *   }
  * }}}
  *
  * @group algebras
  * @groupname types Types
  * @groupdesc types Types introduced by the algebra
  * @groupprio types 1
  * @groupname operations Operations
  * @groupdesc operations Operations creating and transforming values
  * @groupprio operations 2
  */
trait JsonSchemas extends TuplesSchemas with PartialInvariantFunctorSyntax {

  /**
    * The JSON schema of a type `A`
    *
    * @note This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]],
    *       [[InvariantFunctorSyntax]], and [[JsonSchemaOps]] classes.
    * @group types
    */
  type JsonSchema[A]

  /**
    * Provides `xmap` and `xmapPartial` operations.
    *
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]]
    */
  implicit def jsonSchemaPartialInvFunctor: PartialInvariantFunctor[JsonSchema]

  /**
    * The JSON schema of a record type (case class) `A`
    *
    * @note This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]],
    *       [[InvariantFunctorSyntax]], and [[RecordOps]] classes.
    * @group types
    */
  type Record[A] <: JsonSchema[A]

  /**
    * Provides `xmap` and `xmapPartial` operations.
    *
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]]
    */
  implicit def recordPartialInvFunctor: PartialInvariantFunctor[Record]

  /**
    * A JSON schema containing the name of the type `A`.
    * Tagged schemas are useful to describe sum types (sealed traits).
    *
    * @note This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]],
    *       [[InvariantFunctorSyntax]], and [[TaggedOps]] classes.
    * @group types
    */
  type Tagged[A] <: JsonSchema[A]

  /**
    * Provides `xmap` and `xmapPartial` operations.
    *
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]]
    */
  implicit def taggedPartialInvFunctor: PartialInvariantFunctor[Tagged]

  /**
    * A JSON schema for enumerations, i.e. types that have a restricted set of values.
    *
    * @note This type has implicit methods provided by the [[EnumOps]] class.
    * @group types
    */
  type Enum[A] <: JsonSchema[A]

  /** Promotes a schema to an enumeration.
    * Decoding fails if the input string does not match the encoded values of any of the possible values.
    * Encoding does never fail, even if the value is not contained in the set of possible values.
    *
    * @group operations
    */
  def enumeration[A](values: Seq[A])(tpe: JsonSchema[A]): Enum[A]

  /**
    * Convenient constructor for enumerations represented by string values.
    * @group operations
    */
  final def stringEnumeration[A](
      values: Seq[A]
  )(encode: A => String)(implicit tpe: JsonSchema[String]): Enum[A] = {
    val encoded = values.map(a => (a, encode(a))).toMap
    val decoded = encoded.map(_.swap)
    assert(
      encoded.size == decoded.size,
      "Enumeration values must have different string representation"
    )
    enumeration(values)(
      tpe.xmapPartial { str =>
        Validated.fromOption(decoded.get(str))(
          s"Invalid value: ${str} ; valid values are: ${values.map(encode).mkString(", ")}"
        )
      }(encode)
    )
  }

  /**
    * A schema for a statically known value.
    *
    * Encoders always produce the given `value`, encoded according to `tpe`.
    * Decoders first try to decode incoming values with the given `tpe` schema,
    * and then check that it is equal to the given `value`.
    *
    * This is useful to model schemas of objects containing extra fields that
    * are absent from their Scala representation. For example, here is a schema
    * for a GeoJSON point:
    *
    * {{{
    *   case class Point(lon: Double, lat: Double)
    *   val pointSchema = (
    *     field("type")(literal("Point")) zip
    *     field[(Double, Double)]("coordinates")
    *   ).xmap(Point.tupled)(p => (p.lon, p.lat))
    * }}}
    *
    * @group operations
    */
  final def literal[A](
      value: A
  )(implicit tpe: JsonSchema[A]): JsonSchema[Unit] =
    (enumeration(value :: Nil)(tpe): JsonSchema[A]).xmap(_ => ())(_ => value)

  /** Annotates the record JSON schema with a name */
  def namedRecord[A](schema: Record[A], name: String): Record[A]

  /** Annotates the tagged JSON schema with a name */
  def namedTagged[A](schema: Tagged[A], name: String): Tagged[A]

  /** Annotates the enum JSON schema with a name */
  def namedEnum[A](schema: Enum[A], name: String): Enum[A]

  /**
    * Captures a lazy reference to a JSON schema currently being defined:
    *
    * {{{
    *   case class Recursive(next: Option[Recursive])
    *   val recursiveSchema: Record[Recursive] = (
    *     optField("next")(lazyRecord(recursiveSchema, "Rec"))
    *   ).xmap(Recursive)(_.next)
    * }}}
    *
    * Interpreters should return a JsonSchema value that does not evaluate
    * the given `schema` unless it is effectively used.
    *
    * @param schema The record JSON schema whose evaluation should be delayed
    * @param name A unique name identifying the schema
    * @group operations
    */
  def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A]

  /**
    * Captures a lazy reference to a JSON schema currently being defined.
    *
    * Interpreters should return a JsonSchema value that does not evaluate
    * the given `schema` unless it is effectively used.
    *
    * @param schema The tagged JSON schema whose evaluation should be delayed
    * @param name A unique name identifying the schema
    * @group operations
    */
  def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A]

  /** The JSON schema of a record with no fields
    * @group operations
    */
  implicit def emptyRecord: Record[Unit]

  /** The JSON schema of a record with a single field `name` of type `A`
    * @group operations
    */
  def field[A](name: String, documentation: Option[String] = None)(
      implicit tpe: JsonSchema[A]
  ): Record[A]

  /** The JSON schema of a record with a single optional field `name` of type `A`
    *
    * Decoder interpreters successfully decode `None` if the field is absent or if
    * it is present but has the value `null`.
    * Decoder interpreters fail if the field is present but contains an invalid
    * value.
    * Encoder interpreters can omit the field or emit a field with a `null` value.
    * Documentation interpreters must mark the field as optional.
    *
    * @group operations
    */
  def optField[A](name: String, documentation: Option[String] = None)(
      implicit tpe: JsonSchema[A]
  ): Record[Option[A]]

  /** Tags a schema for type `A` with the given tag name */
  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A]

  /** Default discriminator field name for sum types.
    *
    * It defaults to "type", but you can override it twofold:
    * - by overriding this field you can change default discriminator name algebra-wide
    * - by using `withDiscriminator` you can specify discriminator field name for specific sum type
    * @group operations
    */
  def defaultDiscriminatorName: String = "type"

  /** Allows to specify name of discriminator field for sum type */
  def withDiscriminatorTagged[A](
      tagged: Tagged[A],
      discriminatorName: String
  ): Tagged[A]

  /** The JSON schema of a coproduct made of the given alternative tagged records */
  def choiceTagged[A, B](
      taggedA: Tagged[A],
      taggedB: Tagged[B]
  ): Tagged[Either[A, B]]

  /** The JSON schema of a record merging the fields of the two given records */
  def zipRecords[A, B](recordA: Record[A], recordB: Record[B])(
      implicit t: Tupler[A, B]
  ): Record[t.Out]

  /** Include an example value within the given record JSON schema */
  def withExampleRecord[A](record: Record[A], example: A): Record[A]

  /** Include an example value within the given tagged JSON schema */
  def withExampleTagged[A](tagged: Tagged[A], example: A): Tagged[A]

  /** Include an example value within the given enum JSON schema */
  def withExampleEnum[A](enum: Enum[A], example: A): Enum[A]

  /** Include an example value within the given JSON schema */
  def withExampleJsonSchema[A](schema: JsonSchema[A], example: A): JsonSchema[A]

  /** Add a description to the given record JSON schema */
  def withDescriptionRecord[A](
      record: Record[A],
      description: String
  ): Record[A]

  /** Add a description to the given tagged JSON schema */
  def withDescriptionTagged[A](
      tagged: Tagged[A],
      description: String
  ): Tagged[A]

  /** Add a description to the given enum JSON schema */
  def withDescriptionEnum[A](enum: Enum[A], description: String): Enum[A]

  /** Add a description to the given JSON schema */
  def withDescriptionJsonSchema[A](
      schema: JsonSchema[A],
      description: String
  ): JsonSchema[A]

  /** Add a title to the given record JSON schema */
  def withTitleRecord[A](record: Record[A], title: String): Record[A]

  /** Add a title to the given tagged JSON schema */
  def withTitleTagged[A](tagged: Tagged[A], title: String): Tagged[A]

  /** Add a title to the given enum JSON schema */
  def withTitleEnum[A](enum: Enum[A], title: String): Enum[A]

  /** Add a title to the given schema */
  def withTitleJsonSchema[A](
      schema: JsonSchema[A],
      title: String
  ): JsonSchema[A]

  /**
    * A schema that can be either `schemaA` or `schemaB`.
    *
    * Documentation interpreter produce a `oneOf` JSON schema.
    * Encoder interpreters forward to either `schemaA` or `schemaB`.
    * Decoder interpreters first try to decode with `schemaA`, and fallback to `schemaB`
    * in case of failure.
    *
    * The difference between this operation and the operation `orElse` on “tagged” schemas
    * is that this operation does not rely on a discriminator field between the alternative
    * schemas. As a consequence, decoding is slower than with “tagged” schemas and provides
    * less precise error messages.
    *
    * @note Be careful to use ''disjoint'' schemas for `A` and `B` (none must be a subtype
    *       of the other), otherwise, a value of type `B` might also be successfully
    *       decoded as a value of type `A`, and this could have surprising consequences.
    */
  def orFallbackToJsonSchema[A, B](
      schemaA: JsonSchema[A],
      schemaB: JsonSchema[B]
  ): JsonSchema[Either[A, B]]

  /**
    * Documentation related methods for annotating schemas. Encoder and decoder
    * interpreters ignore this information.
    */
  sealed trait JsonSchemaDocumentationOps[A] {
    type Self <: JsonSchema[A]

    /**
      * Include an example of value in this schema. Documentation interpreters
      * can show this example value. Encoder and decoder interpreters ignore
      * this value.
      *
      * @param example Example value to attach to the schema
      */
    def withExample(example: A): Self

    /**
      * Include a description of what this schema represents. Documentation
      * interpreters can show this description. Encoder and decoder interpreters
      * ignore this description.
      *
      * @param description information about the values described by the schema
      */
    def withDescription(description: String): Self

    /**
      * Include a title for the schema. Documentation interpreters can show
      * this title. Encoder and decoder interpreters ignore the title.
      *
      * @param title short title to attach to the schema
      */
    def withTitle(title: String): Self
  }

  /**
    * Implicit methods for values of type [[JsonSchema]]
    * @group operations
    */
  final implicit class JsonSchemaOps[A](schemaA: JsonSchema[A])
      extends JsonSchemaDocumentationOps[A] {
    type Self = JsonSchema[A]

    def withExample(example: A): JsonSchema[A] =
      withExampleJsonSchema(schemaA, example)

    def withDescription(description: String): JsonSchema[A] =
      withDescriptionJsonSchema(schemaA, description)

    def withTitle(title: String): JsonSchema[A] =
      withTitleJsonSchema(schemaA, title)

    /**
      * A schema that can be either `schemaA` or `schemaB`.
      *
      * Documentation interpreter produce a `oneOf` JSON schema.
      * Encoder interpreters forward to either `schemaA` or `schemaB`.
      * Decoder interpreters first try to decode with `schemaA`, and fallback to `schemaB`
      * in case of failure.
      *
      * The difference between this operation and the operation `orElse` on “tagged” schemas
      * is that this operation does not rely on a discriminator field between the alternative
      * schemas. As a consequence, decoding is slower than with “tagged” schemas and provides
      * less precise error messages.
      *
      * @note Be careful to use ''disjoint'' schemas for `A` and `B` (none must be a subtype
      *       of the other), otherwise, a value of type `B` might also be successfully
      *       decoded as a value of type `A`, and this could have surprising consequences.
      * @param schemaB fallback schema
      */
    def orFallbackTo[B](schemaB: JsonSchema[B]): JsonSchema[Either[A, B]] =
      orFallbackToJsonSchema(schemaA, schemaB)
  }

  /** Implicit methods for values of type [[Record]]
    * @group operations
    */
  final implicit class RecordOps[A](recordA: Record[A])
      extends JsonSchemaDocumentationOps[A] {
    type Self = Record[A]

    /** Merge the fields of `recordA` with the fields of `recordB` */
    def zip[B](recordB: Record[B])(implicit t: Tupler[A, B]): Record[t.Out] =
      zipRecords(recordA, recordB)

    def tagged(tag: String): Tagged[A] = taggedRecord(recordA, tag)

    /**
      * Give a name to the schema.
      * Documentation interpreters use that name to refer to this schema.
      * Encoder and decoder interpreters ignore the name.
      *
      * @note Names are used by documentation interpreters to construct
      *       references and the JSON schema specification requires these
      *       to be valid URI's. Consider using `withTitle` if you just want
      *       to override the heading displayed in documentation.
      */
    def named(name: String): Record[A] = namedRecord(recordA, name)

    def withExample(example: A): Record[A] =
      withExampleRecord(recordA, example)

    def withDescription(description: String): Record[A] =
      withDescriptionRecord(recordA, description)

    def withTitle(title: String): Record[A] =
      withTitleRecord(recordA, title)
  }

  /** @group operations */
  final implicit class TaggedOps[A](taggedA: Tagged[A])
      extends JsonSchemaDocumentationOps[A] {
    type Self = Tagged[A]

    def orElse[B](taggedB: Tagged[B]): Tagged[Either[A, B]] =
      choiceTagged(taggedA, taggedB)

    /**
      * Give a name to the schema.
      * Documentation interpreters use that name to refer to this schema.
      * Encoder and decoder interpreters ignore the name.
      *
      * @note Names are used by documentation interpreters to construct
      *       references and the JSON schema specification requires these
      *       to be valid URI's. Consider using `withTitle` if you just want
      *       to override the heading displayed in documentation.
      */
    def named(name: String): Tagged[A] = namedTagged(taggedA, name)

    /**
      * Override the name of the type discriminator field of this record.
      */
    def withDiscriminator(name: String): Tagged[A] =
      withDiscriminatorTagged(taggedA, name)

    def withExample(example: A): Tagged[A] =
      withExampleTagged(taggedA, example)

    def withDescription(description: String): Tagged[A] =
      withDescriptionTagged(taggedA, description)

    def withTitle(title: String): Tagged[A] =
      withTitleTagged(taggedA, title)
  }

  /** @group operations */
  final implicit class EnumOps[A](enumA: Enum[A])
      extends JsonSchemaDocumentationOps[A] {
    type Self = Enum[A]

    /**
      * Give a name to the schema.
      * Documentation interpreters use that name to refer to this schema.
      * Encoder and decoder interpreters ignore the name.
      *
      * @note Names are used by documentation interpreters to construct
      *       references and the JSON schema specification requires these
      *       to be valid URI's. Consider using `withTitle` if you just want
      *       to override the heading displayed in documentation.
      */
    def named(name: String): Enum[A] = namedEnum(enumA, name)

    def withExample(example: A): Enum[A] =
      withExampleEnum(enumA, example)

    def withDescription(description: String): Enum[A] =
      withDescriptionEnum(enumA, description)

    def withTitle(title: String): Enum[A] =
      withTitleEnum(enumA, title)
  }

  /** A JSON schema for type `UUID`
    * @group operations
    */
  final implicit lazy val uuidJsonSchema: JsonSchema[UUID] =
    stringJsonSchema(format = Some("uuid")).xmapPartial { str =>
      Validated.fromEither(
        Exception.nonFatalCatch
          .either(UUID.fromString(str))
          .left
          .map(_ => s"Invalid UUID value: '$str'" :: Nil)
      )
    }(_.toString)

  /**
    * A JSON schema for type `String`.
    *
    * @param format An additional semantic information about the underlying format of the string
    * @see [[https://json-schema.org/understanding-json-schema/reference/string.html#format]]
    * @group operations
    */
  def stringJsonSchema(format: Option[String]): JsonSchema[String]

  /** A JSON schema for type `String`
    * @group operations
    */
  final implicit def defaultStringJsonSchema: JsonSchema[String] =
    stringJsonSchema(format = None)

  /** A JSON schema for type `Int`
    * @group operations
    */
  implicit def intJsonSchema: JsonSchema[Int]

  /** A JSON schema for type `Long`
    * @group operations
    */
  implicit def longJsonSchema: JsonSchema[Long]

  /** A JSON schema for type `BigDecimal`
    * @group operations
    */
  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal]

  /** A JSON schema for type `Float`
    * @group operations
    */
  implicit def floatJsonSchema: JsonSchema[Float]

  /** A JSON schema for type `Double`
    * @group operations
    */
  implicit def doubleJsonSchema: JsonSchema[Double]

  /** A JSON schema for type `Boolean`
    * @group operations
    */
  implicit def booleanJsonSchema: JsonSchema[Boolean]

  /** A JSON schema for type `Byte`
    * @group operations
    */
  implicit def byteJsonSchema: JsonSchema[Byte]

  /** A JSON schema for sequences
    * @group operations
    */
  implicit def arrayJsonSchema[C[X] <: Seq[X], A](
      implicit
      jsonSchema: JsonSchema[A],
      factory: Factory[A, C[A]]
  ): JsonSchema[C[A]]

  /** A JSON schema for maps with string keys
    * @group operations
    */
  implicit def mapJsonSchema[A](
      implicit jsonSchema: JsonSchema[A]
  ): JsonSchema[Map[String, A]]

}
