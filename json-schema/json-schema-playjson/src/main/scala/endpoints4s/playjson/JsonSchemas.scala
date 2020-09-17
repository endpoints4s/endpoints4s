package endpoints4s.playjson

import endpoints4s.{PartialInvariantFunctor, Tupler, Validated, algebra}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.compat._

/**
  * An interpreter for [[endpoints4s.algebra.JsonSchemas]] that produces Play JSON `play.api.libs.json.Reads`
  * and `play.api.libs.json.Writes`.
  */
trait JsonSchemas extends algebra.NoDocsJsonSchemas with TuplesSchemas {

  trait JsonSchema[A] {
    def reads: Reads[A]
    def writes: Writes[A]
  }

  object JsonSchema {
    def apply[A](_reads: Reads[A], _writes: Writes[A]): JsonSchema[A] =
      new JsonSchema[A] {
        def reads: Reads[A] = _reads
        def writes: Writes[A] = _writes
      }
    implicit def toPlayJsonFormat[A](implicit
        jsonSchema: JsonSchema[A]
    ): Format[A] =
      Format(jsonSchema.reads, jsonSchema.writes)
  }

  implicit def jsonSchemaPartialInvFunctor: PartialInvariantFunctor[JsonSchema] =
    new PartialInvariantFunctor[JsonSchema] {
      def xmapPartial[A, B](
          fa: JsonSchema[A],
          f: A => Validated[B],
          g: B => A
      ): JsonSchema[B] =
        JsonSchema(
          fa.reads.flatMap(a =>
            f(a).fold(
              Reads.pure(_),
              errors => Reads.failed(errors.mkString(". "))
            )
          ),
          fa.writes.contramap(g)
        )
      override def xmap[A, B](
          fa: JsonSchema[A],
          f: A => B,
          g: B => A
      ): JsonSchema[B] =
        JsonSchema(fa.reads.map(f), fa.writes.contramap(g))
    }

  trait Record[A] extends JsonSchema[A] {
    override def writes: OWrites[A]
  }

  object Record {
    def apply[A](_reads: Reads[A], _writes: OWrites[A]): Record[A] =
      new Record[A] {
        def reads: Reads[A] = _reads
        def writes: OWrites[A] = _writes
      }
    implicit def toPlayJsonOFormat[A](implicit record: Record[A]): OFormat[A] =
      OFormat(record.reads, record.writes)
  }

  implicit def recordPartialInvFunctor: PartialInvariantFunctor[Record] =
    new PartialInvariantFunctor[Record] {
      def xmapPartial[A, B](
          fa: Record[A],
          f: A => Validated[B],
          g: B => A
      ): Record[B] =
        Record(
          fa.reads.flatMap(a =>
            f(a).fold(
              Reads.pure(_),
              errors => Reads.failed(errors.mkString(". "))
            )
          ),
          fa.writes.contramap(g)
        )
      override def xmap[A, B](fa: Record[A], f: A => B, g: B => A): Record[B] =
        Record(fa.reads.map(f), fa.writes.contramap(g))
    }

  type Enum[A] = JsonSchema[A]

  def enumeration[A](values: Seq[A])(jsonSchema: JsonSchema[A]): Enum[A] = {
    JsonSchema(
      jsonSchema.reads.flatMap { a =>
        if (values.contains(a)) {
          Reads.pure(a)
        } else {
          Reads.failed(
            s"Invalid value: ${Json.stringify(jsonSchema.writes.writes(a))} ; valid values are: ${values
              .map(a => Json.stringify(jsonSchema.writes.writes(a)))
              .mkString(", ")}"
          )
        }
      },
      jsonSchema.writes
    )
  }

  private def lazySchema[A](
      schema: => JsonSchema[A]
  ): JsonSchema[A] = {
    // The schema won’t be evaluated until its `reads` or `writes` is effectively used
    lazy val evaluatedSchema = schema
    new JsonSchema[A] {
      def reads: Reads[A] = Reads(js => evaluatedSchema.reads.reads(js))
      def writes: Writes[A] = Writes(a => evaluatedSchema.writes.writes(a))
    }
  }

  def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A] =
    lazySchema(schema)
  def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A] =
    lazySchema(schema)

  def emptyRecord: Record[Unit] =
    Record(
      new Reads[Unit] {
        def reads(json: JsValue): JsResult[Unit] =
          json match {
            case JsObject(_) => JsSuccess(())
            case _           => JsError(s"Invalid JSON object: $json")
          }
      },
      new OWrites[Unit] {
        def writes(o: Unit): JsObject = Json.obj()
      }
    )

  def field[A](name: String, documentation: Option[String] = None)(implicit
      tpe: JsonSchema[A]
  ): Record[A] =
    Record(
      (__ \ name).read(tpe.reads),
      (__ \ name).write(tpe.writes)
    )

  def optField[A](name: String, documentation: Option[String] = None)(implicit
      tpe: JsonSchema[A]
  ): Record[Option[A]] =
    Record(
      (__ \ name).readNullable(tpe.reads),
      (__ \ name).writeNullable(tpe.writes)
    )

  def orFallbackToJsonSchema[A, B](
      schemaA: JsonSchema[A],
      schemaB: JsonSchema[B]
  ): JsonSchema[Either[A, B]] = {
    val reads =
      schemaA.reads
        .map[Either[A, B]](Left(_))
        .orElse(schemaB.reads.map[Either[A, B]](Right(_)))
        .orElse(Reads(json => JsError(s"Invalid value: $json")))
    val writes =
      Writes[Either[A, B]] {
        case Left(a)  => schemaA.writes.writes(a)
        case Right(b) => schemaB.writes.writes(b)
      }
    JsonSchema(reads, writes)
  }

  def stringJsonSchema(format: Option[String]): JsonSchema[String] =
    JsonSchema(implicitly, implicitly)

  implicit def intJsonSchema: JsonSchema[Int] =
    JsonSchema(implicitly, implicitly)

  implicit def longJsonSchema: JsonSchema[Long] =
    JsonSchema(implicitly, implicitly)

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] =
    JsonSchema(implicitly, implicitly)

  implicit def floatJsonSchema: JsonSchema[Float] =
    JsonSchema(implicitly, implicitly)

  implicit def doubleJsonSchema: JsonSchema[Double] =
    JsonSchema(implicitly, implicitly)

  implicit def booleanJsonSchema: JsonSchema[Boolean] =
    JsonSchema(implicitly, implicitly)

  implicit def byteJsonSchema: JsonSchema[Byte] =
    JsonSchema(implicitly, implicitly)

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit
      jsonSchema: JsonSchema[A],
      factory: Factory[A, C[A]]
  ): JsonSchema[C[A]] =
    JsonSchema[C[A]](
      Reads.traversableReads(factory, jsonSchema.reads),
      Writes.iterableWrites2[A, C[A]](implicitly, jsonSchema.writes)
    )

  implicit def mapJsonSchema[A](implicit
      jsonSchema: JsonSchema[A]
  ): JsonSchema[Map[String, A]] =
    JsonSchema(
      Reads.mapReads(jsonSchema.reads),
      Writes.genericMapWrites(jsonSchema.writes)
    )

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B])(implicit
      t: Tupler[A, B]
  ): Record[t.Out] = {
    val reads = (recordA.reads and recordB.reads).tupled.map { case (a, b) =>
      t(a, b)
    }
    val writes = new OWrites[t.Out] {
      override def writes(o: t.Out): JsObject =
        t.unapply(o) match {
          case (a, b) =>
            recordA.writes.writes(a) deepMerge recordB.writes.writes(b)
        }
    }
    Record(reads, writes)
  }

  trait Tagged[A] extends Record[A] {
    def discriminator: String = defaultDiscriminatorName
    def tagAndJson(a: A): (String, JsObject)
    def findReads(tagName: String): Option[Reads[A]]

    final def reads: Reads[A] = {
      case jsObject @ JsObject(kvs) =>
        kvs.get(discriminator) match {
          case Some(JsString(tag)) =>
            findReads(tag) match {
              case Some(reads) => reads.reads(jsObject)
              case None        => JsError(s"no Reads for tag '$tag': $jsObject")
            }
          case _ =>
            JsError(
              s"expected discriminator field '$discriminator', but not found in: $jsObject"
            )
        }
      case json =>
        JsError(s"expected JSON object for tagged type, but found: $json")
    }

    final def writes: OWrites[A] =
      new OWrites[A] {
        override def writes(a: A): JsObject = {
          val (tag, json) = tagAndJson(a)
          json + (discriminator -> JsString(tag))
        }
      }
  }

  implicit def taggedPartialInvFunctor: PartialInvariantFunctor[Tagged] =
    new PartialInvariantFunctor[Tagged] {
      def xmapPartial[A, B](
          fa: Tagged[A],
          f: A => Validated[B],
          g: B => A
      ): Tagged[B] =
        new Tagged[B] {
          def tagAndJson(b: B): (String, JsObject) = fa.tagAndJson(g(b))
          def findReads(tag: String): Option[Reads[B]] =
            fa.findReads(tag)
              .map(
                _.flatMap(a =>
                  f(a).fold(
                    Reads.pure(_),
                    errors => Reads.failed(errors.mkString(". "))
                  )
                )
              )
        }
      override def xmap[A, B](fa: Tagged[A], f: A => B, g: B => A): Tagged[B] =
        new Tagged[B] {
          def tagAndJson(b: B): (String, JsObject) = fa.tagAndJson(g(b))
          def findReads(tag: String): Option[Reads[B]] =
            fa.findReads(tag).map(_.map(f))
        }
    }

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged[A] {
      def tagAndJson(a: A): (String, JsObject) = (tag, recordA.writes.writes(a))
      def findReads(tagName: String): Option[Reads[A]] =
        if (tag == tagName) Some(recordA.reads) else None
    }

  def withDiscriminatorTagged[A](
      tagged: Tagged[A],
      discriminatorName: String
  ): Tagged[A] =
    new Tagged[A] {
      override def discriminator: String = discriminatorName
      def tagAndJson(a: A): (String, JsObject) = tagged.tagAndJson(a)
      def findReads(tagName: String): Option[Reads[A]] =
        tagged.findReads(tagName)
    }

  def choiceTagged[A, B](
      taggedA: Tagged[A],
      taggedB: Tagged[B]
  ): Tagged[Either[A, B]] =
    new Tagged[Either[A, B]] {
      def tagAndJson(aOrB: Either[A, B]): (String, JsObject) =
        aOrB match {
          case Left(a)  => taggedA.tagAndJson(a)
          case Right(b) => taggedB.tagAndJson(b)
        }

      def findReads(tagName: String): Option[Reads[Either[A, B]]] =
        taggedA.findReads(tagName).map(_.map[Either[A, B]](Left(_))) orElse
          taggedB.findReads(tagName).map(_.map[Either[A, B]](Right(_)))
    }

}
