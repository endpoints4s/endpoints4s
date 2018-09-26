package endpoints.playjson

import endpoints.algebra
import play.api.libs.json._

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scala.util.Try

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

  type Record[A] = JsonSchema[A]

  def emptyRecord: Record[Unit] =
    JsonSchema(
      new Reads[Unit] {
        override def reads(json: JsValue): JsResult[Unit] = json match {
          case JsObject(_) => JsSuccess(())
          case _ => JsError("expected JSON object, but found: " + json)
        }
      },
      new Writes[Unit] {
        override def writes(o: Unit): JsValue = Json.obj()
      }
    )

  def field[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[A] =
    JsonSchema(
      (__ \ name).read(tpe.reads),
      (__ \ name).write(tpe.writes)
    )

  def optField[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    JsonSchema(
      (__ \ name).readNullable(tpe.reads),
      (__ \ name).writeNullable(tpe.writes)
    )

  implicit def stringJsonSchema: JsonSchema[String] = JsonSchema(implicitly, implicitly)

  implicit def intJsonSchema: JsonSchema[Int] = JsonSchema(implicitly, implicitly)

  implicit def longJsonSchema: JsonSchema[Long] = JsonSchema(implicitly, implicitly)

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] = JsonSchema(implicitly, implicitly)

  implicit def doubleJsonSchema: JsonSchema[Double] = JsonSchema(implicitly, implicitly)

  implicit def booleanJsonSchema: JsonSchema[Boolean] = JsonSchema(implicitly, implicitly)

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit jsonSchema: JsonSchema[A], cbf: CanBuildFrom[_, A, C[A]]): JsonSchema[C[A]] =
    JsonSchema(
      new Reads[C[A]] {
        override def reads(json: JsValue): JsResult[C[A]] = json match {
          case JsArray(values) =>
            val builder = cbf()
            builder.sizeHint(values)

            Try {
              values.foldLeft(builder) { case (acc, value) =>
                acc += value.as[A](jsonSchema.reads)
              }
            }.transform(
              builder => Try(JsSuccess(builder.result())),
              error => Try(JsError(error.getMessage))
            ).get
          case other => JsError("expected JsArray, but was: " + other)
        }
      },
      Writes.traversableWrites(jsonSchema.writes)
    )

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B]): Record[(A, B)] = {
    val reads = {
      import play.api.libs.functional.syntax._
      (recordA.reads and recordB.reads).tupled
    }
    val writes = new Writes[(A, B)] {
      override def writes(o: (A, B)): JsValue = o match {
        case (a, b) => recordA.writes.writes(a).asInstanceOf[JsObject] deepMerge recordB.writes.writes(b).asInstanceOf[JsObject]
      }
    }
    JsonSchema(reads, writes)
  }

  def invmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] = invmapJsonSchema(record, f, g)

  def invmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
    JsonSchema(
      new Reads[B] {
        override def reads(json: JsValue): JsResult[B] = jsonSchema.reads.reads(json).map(f)
      },
      new Writes[B] {
        override def writes(b: B): JsValue = jsonSchema.writes.writes(g(b))
      }
    )

  trait Tagged[A] extends Record[A] {
    def tagAndJson(a: A): (String, JsValue)
    def findReads(tagName: String): Option[Reads[A]]

    def reads: Reads[A] = new Reads[A] {
      override def reads(json: JsValue): JsResult[A] = json match {
        case JsObject(kvs) => if (kvs.size == 1) {
          val (key, value) = kvs.toList.head
          findReads(key) match {
            case Some(reads) => reads.reads(value)
            case None => JsError(s"no Reads for tag '$key': $json")
          }
        } else JsError(s"expected exactly one tag, but found ${kvs.size}: $json")
        case _ => JsError(s"expected JSON object for tagged type, but found: $json")
      }
    }

    def writes: Writes[A] = new Writes[A] {
      override def writes(a: A): JsValue = {
        val (tag, json) = tagAndJson(a)
        Json.obj(tag -> json)
      }
    }
  }

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] = new Tagged[A] {
    def tagAndJson(a: A): (String, JsValue) = (tag, recordA.writes.writes(a))
    def findReads(tagName: String): Option[Reads[A]] = if (tag == tagName) Some(recordA.reads) else None
  }

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] = new Tagged[Either[A, B]] {
    def tagAndJson(aOrB: Either[A, B]): (String, JsValue) = aOrB match {
      case Left(a) => taggedA.tagAndJson(a)
      case Right(b) => taggedB.tagAndJson(b)
    }

    def findReads(tagName: String): Option[Reads[Either[A, B]]] =
      taggedA.findReads(tagName).map(_.map[Either[A, B]](Left(_))) orElse
        taggedB.findReads(tagName).map(_.map[Either[A, B]](Right(_)))
  }

  def invmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] = new Tagged[B] {
    def tagAndJson(b: B): (String, JsValue) = tagged.tagAndJson(g(b))
    def findReads(tag: String): Option[Reads[B]] = tagged.findReads(tag).map(_.map(f))
  }

}
