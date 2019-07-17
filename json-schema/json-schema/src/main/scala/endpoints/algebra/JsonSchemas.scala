package endpoints.algebra

import java.util.UUID

import scala.collection.compat._
import scala.language.higherKinds

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
  */
trait JsonSchemas {

  /** The JSON schema of a type `A` */
  type JsonSchema[A]

  /** The JSON schema of a record type (case class) `A` */
  type Record[A] <: JsonSchema[A]

  /** A JSON schema containing the name of the type `A`.
    * Tagged schemas are useful to describe sum types (sealed traits).
    */
  type Tagged[A] <: JsonSchema[A]

  /** A JSON schema for enumerations, i.e. types that have a restricted set of values. */
  type Enum[A] <: JsonSchema[A]

  /** Promotes a schema to an enumeration and converts between enum constants and JSON strings.
    * Decoding fails if the input string does not match the encoded values of any of the possible values.
    * Encoding does never fail, even if the value is not contained in the set of possible values.
    * */
  def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: JsonSchema[String]): Enum[A]

  /** Annotates JSON schema with a name */
  def named[A, S[T] <: JsonSchema[T]](schema: S[A], name: String): S[A]

  /**
    * Captures a lazy reference to a JSON schema currently being defined:
    *
    * {{{
    *   case class Rec(next: Option[Rec])
    *   val recSchema: JsonSchema[Rec] = (
    *     optField("next")(lazySchema(recSchema, "Rec"))
    *   ).xmap(Rec)(_.next)
    * }}}
    *
    * Interpreters should return a JsonSchema value that does not evaluate
    * the given `schema` unless it is effectively used.
    *
    * @param schema The JSON schema whose evaluation should be delayed
    * @param name A unique name identifying the schema
    */
  def lazySchema[A](schema: => JsonSchema[A], name: String): JsonSchema[A]

  /** The JSON schema of a record with no fields */
  def emptyRecord: Record[Unit]

  /** The JSON schema of a record with a single field `name` of type `A` */
  def field[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[A]

  /** The JSON schema of a record with a single optional field `name` of type `A` */
  def optField[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[Option[A]]

  /** Tags a schema for type `A` with the given tag name */
  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A]

  /** Default discriminator field name for sum types.
    *
    * It defaults to "type", but you can override it twofold:
    * - by overriding this field you can change default discriminator name algebra-wide
    * - by using `withDiscriminator` you can specify discriminator field name for specific sum type
    * */
  def defaultDiscriminatorName: String = "type"

  /** Allows to specify name of discriminator field for sum type */
  def withDiscriminator[A](tagged: Tagged[A], discriminatorName: String): Tagged[A]

  /** The JSON schema of a coproduct made of the given alternative tagged records */
  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]]

  /** The JSON schema of a record merging the fields of the two given records */
  def zipRecords[A, B](recordA: Record[A], recordB: Record[B]): Record[(A, B)]

  /** Transforms the type of the JSON schema */
  def xmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B]

  /** Transforms the type of the JSON schema */
  def xmapTagged[A, B](taggedA: Tagged[A], f: A => B, g: B => A): Tagged[B]

  /** Transforms the type of the JSON schema */
  def xmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B]

  /** Convenient infix operations */
  final implicit class RecordOps[A](recordA: Record[A]) {
    def zip[B](recordB: Record[B]): Record[(A, B)] = zipRecords(recordA, recordB)
    def xmap[B](f: A => B)(g: B => A): Record[B] = xmapRecord(recordA, f, g)
    def tagged(tag: String): Tagged[A] = taggedRecord(recordA, tag)
  }

  /** Convenient infix operations */
  final implicit class JsonSchemaOps[A](jsonSchema: JsonSchema[A]) {
    def xmap[B](f: A => B)(g: B => A): JsonSchema[B] = xmapJsonSchema(jsonSchema, f, g)
  }

  final implicit class TaggedOps[A](taggedA: Tagged[A]) {
    def orElse[B](taggedB: Tagged[B]): Tagged[Either[A, B]] = choiceTagged(taggedA, taggedB)
    def xmap[B](f: A => B)(g: B => A): Tagged[B] = xmapTagged(taggedA, f, g)
  }

  /** A JSON schema for type `UUID` */
  implicit def uuidJsonSchema: JsonSchema[UUID]

  /** A JSON schema for type `String` */
  implicit def stringJsonSchema: JsonSchema[String]

  /** A JSON schema for type `Int` */
  implicit def intJsonSchema: JsonSchema[Int]

  /** A JSON schema for type `Long` */
  implicit def longJsonSchema: JsonSchema[Long]

  /** A JSON schema for type `BigDecimal` */
  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal]

  /** A JSON schema for type `Float` */
  implicit def floatJsonSchema: JsonSchema[Float]

  /** A JSON schema for type `Double` */
  implicit def doubleJsonSchema: JsonSchema[Double]

  /** A JSON schema for type `Boolean` */
  implicit def booleanJsonSchema: JsonSchema[Boolean]

  /** A JSON schema for type `Byte` */
  implicit def byteJsonSchema: JsonSchema[Byte]

  /** A JSON schema for sequences */
  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit
    jsonSchema: JsonSchema[A],
    factory: Factory[A, C[A]]
  ): JsonSchema[C[A]]

  /** A JSON schema for maps with string keys */
  implicit def mapJsonSchema[A](implicit jsonSchema: JsonSchema[A]): JsonSchema[Map[String, A]]

}
