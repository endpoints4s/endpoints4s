package endpoints.playjson

import java.util.UUID

import endpoints.algebra
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.compat._
import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces Play JSON [[play.api.libs.json.Reads]]
  * and [[play.api.libs.json.Writes]].
  */
trait JsonSchemas
  extends algebra.JsonSchemas {

  trait JsonSchema[A] {
    def reads: Reads[A]
    def writes: Writes[A]
  }

  object JsonSchema {
    def apply[A](_reads: Reads[A], _writes: Writes[A]): JsonSchema[A] = new JsonSchema[A] {
      def reads: Reads[A] = _reads
      def writes: Writes[A] = _writes
    }
    implicit def toPlayJsonFormat[A](implicit jsonSchema: JsonSchema[A]): Format[A] =
      Format(jsonSchema.reads, jsonSchema.writes)
  }

  trait Record[A] extends JsonSchema[A] {
    override def writes: OWrites[A]
  }

  object Record {
    def apply[A](_reads: Reads[A], _writes: OWrites[A]): Record[A] = new Record[A] {
      def reads: Reads[A] = _reads
      def writes: OWrites[A] = _writes
    }
    implicit def toPlayJsonOFormat[A](implicit record: Record[A]): OFormat[A] =
      OFormat(record.reads, record.writes)
  }

  type Enum[A] = JsonSchema[A]

  def enumeration[A](values: Seq[A])(encode: A => String)(implicit jsonSchema: JsonSchema[String]): Enum[A] = {
    lazy val stringToEnum: Map[String, A] = values.map(value => (encode(value), value)).toMap
    def decode(string: String): Either[String, A] = stringToEnum.get(string).toRight("Cannot decode as enum value: " + string)

    JsonSchema(
      new Reads[A] {
        override def reads(json: JsValue): JsResult[A] = {
          jsonSchema.reads.reads(json).flatMap(value => decode(value).fold(JsError(_), JsSuccess(_)))
        }
      },
      jsonSchema.writes.contramap(encode)
    )
  }


  def named[A, S[T] <: JsonSchema[T]](schema: S[A], name: String): S[A] = schema

  def lazySchema[A](schema: => JsonSchema[A], name: String): JsonSchema[A] = {
    // The schema won’t be evaluated until its `reads` or `writes` is effectively used
    lazy val evaluatedSchema = schema
    new JsonSchema[A] {
      def reads: Reads[A] = Reads(js => evaluatedSchema.reads.reads(js))
      def writes: Writes[A] = Writes(a => evaluatedSchema.writes.writes(a))
    }
  }

  def emptyRecord: Record[Unit] =
    Record(
      new Reads[Unit] {
        override def reads(json: JsValue): JsResult[Unit] = json match {
          case JsObject(_) => JsSuccess(())
          case _ => JsError("expected JSON object, but found: " + json)
        }
      },
      new OWrites[Unit] {
        override def writes(o: Unit): JsObject = Json.obj()
      }
    )

  def field[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[A] =
    Record(
      (__ \ name).read(tpe.reads),
      (__ \ name).write(tpe.writes)
    )

  def optField[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    Record(
      (__ \ name).readNullable(tpe.reads),
      (__ \ name).writeNullable(tpe.writes)
    )

  implicit def uuidJsonSchema: JsonSchema[UUID] = JsonSchema(implicitly, implicitly)

  implicit def stringJsonSchema: JsonSchema[String] = JsonSchema(implicitly, implicitly)

  implicit def intJsonSchema: JsonSchema[Int] = JsonSchema(implicitly, implicitly)

  implicit def longJsonSchema: JsonSchema[Long] = JsonSchema(implicitly, implicitly)

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] = JsonSchema(implicitly, implicitly)

  implicit def floatJsonSchema: JsonSchema[Float] = JsonSchema(implicitly, implicitly)

  implicit def doubleJsonSchema: JsonSchema[Double] = JsonSchema(implicitly, implicitly)

  implicit def booleanJsonSchema: JsonSchema[Boolean] = JsonSchema(implicitly, implicitly)

  implicit def byteJsonSchema: JsonSchema[Byte] = JsonSchema(implicitly, implicitly)

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit jsonSchema: JsonSchema[A], factory: Factory[A, C[A]]): JsonSchema[C[A]] =
    JsonSchema(
      new Reads[C[A]] {
        override def reads(json: JsValue): JsResult[C[A]] = json match {
          case JsArray(values) =>
            val builder = factory.newBuilder
            builder.sizeHint(values)
            values.foldLeft[JsResult[collection.mutable.Builder[A, C[A]]]](JsSuccess(builder)) {
              case (acc, value) => (acc and jsonSchema.reads.reads(value))((b, a) => b += a)
            }.map(_.result())
          case other => JsError("expected JsArray, but was: " + other)
        }
      },
      Writes.traversableWrites(jsonSchema.writes)
    )

  implicit def mapJsonSchema[A](implicit jsonSchema: JsonSchema[A]): JsonSchema[Map[String, A]] =
    JsonSchema(
      Reads.mapReads(jsonSchema.reads),
      Writes.mapWrites(jsonSchema.writes)
    )

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B]): Record[(A, B)] = {
    val reads = (recordA.reads and recordB.reads).tupled
    val writes = new OWrites[(A, B)] {
      override def writes(o: (A, B)): JsObject = o match {
        case (a, b) => recordA.writes.writes(a) deepMerge recordB.writes.writes(b)
      }
    }
    Record(reads, writes)
  }

  def xmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] =
    Record(record.reads.map(f), record.writes.contramap(g))

  def xmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
    JsonSchema(jsonSchema.reads.map(f), jsonSchema.writes.contramap(g))

  trait Tagged[A] extends Record[A] {
    def discriminator: String = defaultDiscriminatorName
    def tagAndJson(a: A): (String, JsObject)
    def findReads(tagName: String): Option[Reads[A]]

    def reads: Reads[A] = new Reads[A] {
      override def reads(json: JsValue): JsResult[A] = json match {
        case jsObject@JsObject(kvs) =>
          kvs.get(discriminator) match {
            case Some(JsString(tag)) =>
              findReads(tag) match {
                case Some(reads) => reads.reads(jsObject)
                case None => JsError(s"no Reads for tag '$tag': $json")
              }
            case _ =>
              JsError(s"expected discriminator field '$discriminator', but not found in: $json")
          }
        case _ => JsError(s"expected JSON object for tagged type, but found: $json")
      }
    }

    def writes: OWrites[A] = new OWrites[A] {
      override def writes(a: A): JsObject = {
        val (tag, json) = tagAndJson(a)
        json + (discriminator -> JsString(tag))
      }
    }
  }

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] = new Tagged[A] {
    def tagAndJson(a: A): (String, JsObject) = (tag, recordA.writes.writes(a))
    def findReads(tagName: String): Option[Reads[A]] = if (tag == tagName) Some(recordA.reads) else None
  }

  def withDiscriminator[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
    new Tagged[A] {
      override def discriminator: String = discriminatorName
      def tagAndJson(a: A): (String, JsObject) = tagged.tagAndJson(a)
      def findReads(tagName: String): Option[Reads[A]] = tagged.findReads(tagName)
    }

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] = new Tagged[Either[A, B]] {
    def tagAndJson(aOrB: Either[A, B]): (String, JsObject) = aOrB match {
      case Left(a) => taggedA.tagAndJson(a)
      case Right(b) => taggedB.tagAndJson(b)
    }

    def findReads(tagName: String): Option[Reads[Either[A, B]]] =
      taggedA.findReads(tagName).map(_.map[Either[A, B]](Left(_))) orElse
        taggedB.findReads(tagName).map(_.map[Either[A, B]](Right(_)))
  }

  def xmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] = new Tagged[B] {
    def tagAndJson(b: B): (String, JsObject) = tagged.tagAndJson(g(b))
    def findReads(tag: String): Option[Reads[B]] = tagged.findReads(tag).map(_.map(f))
  }
}
