package endpoints4s
package circe

import endpoints4s.algebra.circe.CirceCodec
import io.circe._

import scala.collection.compat._

/** An interpreter for [[endpoints4s.algebra.JsonSchemas]] that produces a circe codec.
  */
trait JsonSchemas extends algebra.NoDocsJsonSchemas with TuplesSchemas {

  trait JsonSchema[A] {
    def encoder: Encoder[A]
    def decoder: Decoder[A]
  }

  implicit def jsonSchemaPartialInvFunctor: PartialInvariantFunctor[JsonSchema] =
    new PartialInvariantFunctor[JsonSchema] {
      def xmapPartial[A, B](
          fa: JsonSchema[A],
          f: A => Validated[B],
          g: B => A
      ): JsonSchema[B] =
        JsonSchema(
          fa.encoder.contramap(g),
          fa.decoder.emap(a => f(a).toEither.left.map(_.mkString(". ")))
        )
      override def xmap[A, B](
          fa: JsonSchema[A],
          f: A => B,
          g: B => A
      ): JsonSchema[B] =
        JsonSchema(fa.encoder.contramap(g), fa.decoder.map(f))
    }

  object JsonSchema {
    def apply[A](_encoder: Encoder[A], _decoder: Decoder[A]): JsonSchema[A] =
      new JsonSchema[A] { def encoder = _encoder; def decoder = _decoder }

    implicit def toCirceCodec[A](implicit
        jsonSchema: JsonSchema[A]
    ): CirceCodec[A] =
      CirceCodec.fromEncoderAndDecoder(jsonSchema.encoder, jsonSchema.decoder)

    implicit def toCirceEncoder[A](implicit
        jsonSchema: JsonSchema[A]
    ): Encoder[A] =
      jsonSchema.encoder

    implicit def toCirceDecoder[A](implicit
        jsonSchema: JsonSchema[A]
    ): Decoder[A] =
      jsonSchema.decoder
  }

  trait Record[A] extends JsonSchema[A] {
    override def encoder: Encoder.AsObject[A]
  }

  object Record {
    def apply[A](
        _encoder: Encoder.AsObject[A],
        _decoder: Decoder[A]
    ): Record[A] =
      new Record[A] { def encoder = _encoder; def decoder = _decoder }

    implicit def toCirceCodec[A](implicit record: Record[A]): CirceCodec[A] =
      CirceCodec.fromEncoderAndDecoder(record.encoder, record.decoder)

    implicit def toCirceObjectEncoder[A](implicit
        record: Record[A]
    ): Encoder.AsObject[A] =
      record.encoder

    implicit def toCirceDecoder[A](implicit record: Record[A]): Decoder[A] =
      record.decoder
  }

  implicit def recordPartialInvFunctor: PartialInvariantFunctor[Record] =
    new PartialInvariantFunctor[Record] {
      def xmapPartial[A, B](
          fa: Record[A],
          f: A => Validated[B],
          g: B => A
      ): Record[B] =
        Record(
          fa.encoder.contramapObject(g),
          fa.decoder.emap(a => f(a).toEither.left.map(_.mkString(". ")))
        )
      override def xmap[A, B](fa: Record[A], f: A => B, g: B => A): Record[B] =
        Record(fa.encoder.contramapObject(g), fa.decoder.map(f))
    }

  trait Tagged[A] extends Record[A] {
    def discriminator: String = defaultDiscriminatorName
    def taggedEncoded(a: A): (String, JsonObject)
    def taggedDecoder(tag: String): Option[Decoder[A]]
    final def encoder: Encoder.AsObject[A] =
      Encoder.AsObject.instance { a =>
        val (tag, json) = taggedEncoded(a)
        (discriminator -> Json.fromString(tag)) +: json
      }
    final def decoder: Decoder[A] =
      Decoder.instance { cursor =>
        cursor.as[JsonObject].flatMap { jsonObject =>
          jsonObject(discriminator).flatMap(_.asString) match {
            case Some(tag) =>
              taggedDecoder(tag) match {
                case Some(dec) => dec.decodeJson(cursor.value)
                case None =>
                  Left(
                    DecodingFailure(
                      s"No decoder for discriminator '$tag'!",
                      Nil
                    )
                  )
              }
            case None =>
              Left(
                DecodingFailure(
                  s"Missing type discriminator field '$discriminator'!",
                  Nil
                )
              )
          }
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
          def taggedEncoded(b: B): (String, JsonObject) = fa.taggedEncoded(g(b))
          def taggedDecoder(tag: String): Option[Decoder[B]] =
            fa.taggedDecoder(tag)
              .map(_.emap(a => f(a).toEither.left.map(_.mkString(". "))))
        }
      override def xmap[A, B](fa: Tagged[A], f: A => B, g: B => A): Tagged[B] =
        new Tagged[B] {
          def taggedEncoded(b: B): (String, JsonObject) = fa.taggedEncoded(g(b))
          def taggedDecoder(tag: String): Option[Decoder[B]] =
            fa.taggedDecoder(tag).map(_.map(f))
        }
    }

  type Enum[A] = JsonSchema[A]

  def enumeration[A](values: Seq[A])(tpe: JsonSchema[A]): Enum[A] = {
    JsonSchema(
      tpe.encoder,
      tpe.decoder.emap { a =>
        if (values.contains(a)) Right(a)
        else
          Left(
            s"Invalid value: ${tpe.encoder(a).spaces2} ; valid values are: ${values
              .map(a => tpe.encoder(a).spaces2)
              .mkString(", ")}"
          )
      }
    )
  }

  override def lazySchema[A](name: String)(schema: => JsonSchema[A]): JsonSchema[A] = {
    // The schema won’t be evaluated until its `encoder` or `decoder` is effectively used
    lazy val evaluatedSchema = schema
    new JsonSchema[A] {
      def encoder: Encoder[A] =
        Encoder.instance(a => evaluatedSchema.encoder(a))
      def decoder: Decoder[A] =
        Decoder.instance(c => evaluatedSchema.decoder(c))
    }
  }

  def lazyRecord[A](schema: => Record[A], name: String): JsonSchema[A] =
    lazySchema(name)(schema)
  def lazyTagged[A](schema: => Tagged[A], name: String): JsonSchema[A] =
    lazySchema(name)(schema)

  override def lazyRecord[A](name: String)(schema: => Record[A]): Record[A] = {
    // The schema won’t be evaluated until its `encoder` or `decoder` is effectively used
    lazy val evaluatedSchema = schema
    Record(
      Encoder.AsObject.instance(a => evaluatedSchema.encoder.encodeObject(a)),
      Decoder.instance(c => evaluatedSchema.decoder(c))
    )
  }

  override def lazyTagged[A](name: String)(schema: => Tagged[A]): Tagged[A] = {
    // The schema won’t be evaluated until its `encoder` or `decoder` is effectively used
    lazy val evaluatedSchema = schema
    new Tagged[A] {
      override def discriminator: String = evaluatedSchema.discriminator
      def taggedEncoded(a: A): (String, JsonObject) =
        evaluatedSchema.taggedEncoded(a)

      def taggedDecoder(tag: String): Option[Decoder[A]] =
        evaluatedSchema.taggedDecoder(tag)
    }
  }

  def emptyRecord: Record[Unit] =
    Record(
      io.circe.Encoder.encodeUnit,
      io.circe.Decoder.instance { cursor =>
        if (cursor.value.isObject) Right(())
        else
          Left(
            DecodingFailure(
              s"Invalid JSON object: ${cursor.value.noSpaces}",
              cursor.history
            )
          )
      }
    )

  def field[A](name: String, documentation: Option[String] = None)(implicit
      tpe: JsonSchema[A]
  ): Record[A] =
    Record(
      io.circe.Encoder.AsObject.instance[A](a => JsonObject.singleton(name, tpe.encoder.apply(a))),
      io.circe.Decoder.instance[A](cursor => tpe.decoder.tryDecode(cursor.downField(name)))
    )

  def optField[A](name: String, documentation: Option[String] = None)(implicit
      tpe: JsonSchema[A]
  ): Record[Option[A]] =
    Record(
      io.circe.Encoder.AsObject.instance[Option[A]](maybeA =>
        JsonObject.fromIterable(maybeA.map(a => name -> tpe.encoder.apply(a)))
      ),
      io.circe.Decoder.instance[Option[A]](cursor =>
        io.circe.Decoder
          .decodeOption(tpe.decoder)
          .tryDecode(cursor.downField(name))
      )
    )

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged[A] {
      def taggedEncoded(a: A) = (tag, recordA.encoder.encodeObject(a))
      def taggedDecoder(tagName: String) =
        if (tag == tagName) Some(recordA.decoder) else None
    }

  def withDiscriminatorTagged[A](
      tagged: Tagged[A],
      discriminatorName: String
  ): Tagged[A] =
    new Tagged[A] {
      override def discriminator: String = discriminatorName
      def taggedEncoded(a: A): (String, JsonObject) = tagged.taggedEncoded(a)
      def taggedDecoder(tag: String): Option[Decoder[A]] =
        tagged.taggedDecoder(tag)
    }

  def choiceTagged[A, B](
      taggedA: Tagged[A],
      taggedB: Tagged[B]
  ): Tagged[Either[A, B]] =
    new Tagged[Either[A, B]] {
      def taggedEncoded(aOrB: Either[A, B]) =
        aOrB match {
          case Left(a)  => taggedA.taggedEncoded(a)
          case Right(b) => taggedB.taggedEncoded(b)
        }
      def taggedDecoder(tag: String) =
        taggedA
          .taggedDecoder(tag)
          .map(_.map[Either[A, B]](Left(_)))
          .orElse(taggedB.taggedDecoder(tag).map(_.map[Either[A, B]](Right(_))))
    }

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B])(implicit
      t: Tupler[A, B]
  ): Record[t.Out] = {
    val encoder =
      io.circe.Encoder.AsObject.instance[t.Out] { o =>
        val (a, b) = t.unapply(o)
        // For some reason, `deepMerge` puts the fields of its left-hand-side *after*
        // the fields of its right-hand-side. Hence the inversion between `recordA`
        // and `recordB`.
        recordB.encoder
          .apply(b)
          .deepMerge(recordA.encoder.apply(a))
          .asObject
          .get
      }
    val decoder =
      recordA.decoder.product(recordB.decoder).map { case (a, b) => t(a, b) }
    Record(encoder, decoder)
  }

  def orFallbackToJsonSchema[A, B](
      schemaA: JsonSchema[A],
      schemaB: JsonSchema[B]
  ): JsonSchema[Either[A, B]] = {
    val encoder: io.circe.Encoder[Either[A, B]] =
      io.circe.Encoder.instance {
        case Left(a)  => schemaA.encoder(a)
        case Right(b) => schemaB.encoder(b)
      }
    val decoder =
      schemaA.decoder
        .map[Either[A, B]](Left(_))
        .or(schemaB.decoder.map[Either[A, B]](Right(_)))
        .withErrorMessage("Invalid value.")
    JsonSchema(encoder, decoder)
  }

  def stringJsonSchema(format: Option[String]): JsonSchema[String] =
    JsonSchema(implicitly, implicitly)

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

  private def getDecoder[A: Decoder: MultipleOf: Ordering](constraints: NumericConstraints[A]) =
    Decoder[A].flatMap { value =>
      Decoder[A].ensure(
        a => constraints.satisfiedBy(a),
        s"$value does not satisfy the constraints: $constraints"
      )
    }

  override def intWithConstraintsJsonSchema(constraints: NumericConstraints[Int]): JsonSchema[Int] =
    JsonSchema(implicitly, getDecoder(constraints))

  override def longWithConstraintsJsonSchema(
      constraints: NumericConstraints[Long]
  ): JsonSchema[Long] =
    JsonSchema(implicitly, getDecoder(constraints))

  override def bigdecimalWithConstraintsJsonSchema(
      constraints: NumericConstraints[BigDecimal]
  ): JsonSchema[BigDecimal] =
    JsonSchema(implicitly, getDecoder(constraints))

  override def floatWithConstraintsJsonSchema(
      constraints: NumericConstraints[Float]
  ): JsonSchema[Float] =
    JsonSchema(implicitly, getDecoder(constraints))

  override def doubleWithConstraintsJsonSchema(
      constraints: NumericConstraints[Double]
  ): JsonSchema[Double] =
    JsonSchema(implicitly, getDecoder(constraints))

  implicit def booleanJsonSchema: JsonSchema[Boolean] =
    JsonSchema(implicitly, implicitly)

  implicit def byteJsonSchema: JsonSchema[Byte] =
    JsonSchema(implicitly, implicitly)

  implicit def arrayJsonSchema[C[X] <: Iterable[X], A](implicit
      jsonSchema: JsonSchema[A],
      factory: Factory[A, C[A]]
  ): JsonSchema[C[A]] =
    JsonSchema(
      io.circe.Encoder.encodeIterable[A, C](jsonSchema.encoder, implicitly),
      io.circe.Decoder.decodeIterable[A, C](jsonSchema.decoder, factory)
    )

  implicit def mapJsonSchema[A](implicit
      jsonSchema: JsonSchema[A]
  ): JsonSchema[Map[String, A]] =
    JsonSchema(
      io.circe.Encoder.encodeMap[String, A](implicitly, jsonSchema.encoder),
      io.circe.Decoder.decodeMap[String, A](implicitly, jsonSchema.decoder)
    )

}
