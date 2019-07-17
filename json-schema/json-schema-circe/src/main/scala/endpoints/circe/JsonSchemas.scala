package endpoints
package circe

import java.util.UUID

import endpoints.algebra.circe.CirceCodec
import io.circe._

import scala.collection.compat._
import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces a circe codec.
  */
trait JsonSchemas
  extends algebra.JsonSchemas {

  trait JsonSchema[A] {
    def encoder: Encoder[A]
    def decoder: Decoder[A]
  }

  object JsonSchema {
    def apply[A](_encoder: Encoder[A], _decoder: Decoder[A]): JsonSchema[A] =
      new JsonSchema[A] { def encoder = _encoder; def decoder = _decoder }

    implicit def toCirceCodec[A](implicit jsonSchema: JsonSchema[A]): CirceCodec[A] =
      CirceCodec.fromEncoderAndDecoder(jsonSchema.encoder, jsonSchema.decoder)

    implicit def toCirceEncoder[A](implicit jsonSchema: JsonSchema[A]): Encoder[A] =
      jsonSchema.encoder

    implicit def toCirceDecoder[A](implicit jsonSchema: JsonSchema[A]): Decoder[A] =
      jsonSchema.decoder
  }

  trait Record[A] extends JsonSchema[A] {
    override def encoder: ObjectEncoder[A]
  }

  object Record {
    def apply[A](_encoder: ObjectEncoder[A], _decoder: Decoder[A]): Record[A] =
      new Record[A] { def encoder = _encoder; def decoder = _decoder }

    implicit def toCirceCodec[A](implicit record: Record[A]): CirceCodec[A] =
      CirceCodec.fromEncoderAndDecoder(record.encoder, record.decoder)

    implicit def toCirceObjectEncoder[A](implicit record: Record[A]): ObjectEncoder[A] =
      record.encoder

    implicit def toCirceDecoder[A](implicit record: Record[A]): Decoder[A] =
      record.decoder
  }

  trait Tagged[A] extends Record[A] {
    def discriminator: String = defaultDiscriminatorName
    def taggedEncoded(a: A): (String, JsonObject)
    def taggedDecoder(tag: String): Option[Decoder[A]]
    def encoder: ObjectEncoder[A] =
      ObjectEncoder.instance { a =>
        val (tag, json) = taggedEncoded(a)
        (discriminator -> Json.fromString(tag)) +: json
      }
    def decoder: Decoder[A] =
      Decoder.instance { cursor =>
        cursor.as[JsonObject].right.flatMap { jsonObject =>
          jsonObject(discriminator).flatMap(_.asString) match {
            case Some(tag) =>
              taggedDecoder(tag) match {
                case Some(dec) => dec.decodeJson(cursor.value)
                case None => Left(DecodingFailure(s"No decoder for discriminator '$tag'!", Nil))
              }
            case None =>
              Left(DecodingFailure(s"Missing type discriminator field '$discriminator'!", Nil))
          }
        }
      }
  }

  type Enum[A] = JsonSchema[A]

  def enumeration[A](values: Seq[A])(encode: A => String)(implicit tpe: JsonSchema[String]): Enum[A] = {
    lazy val stringToEnum: Map[String, A] = values.map(value => (encode(value), value)).toMap
    def decode(string: String): Either[String, A] = stringToEnum.get(string).toRight("Cannot decode as enum value: " + string)

    JsonSchema(
      tpe.encoder.contramap(encode),
      tpe.decoder.emap(decode)
    )
  }

  def named[A, S[T] <: JsonSchema[T]](schema: S[A], name: String): S[A] = schema

  def lazySchema[A](schema: => JsonSchema[A], name: String): JsonSchema[A] = {
    // The schema won’t be evaluated until its `encoder` or `decoder` is effectively used
    lazy val evaluatedSchema = schema
    new JsonSchema[A] {
      def encoder: Encoder[A] = Encoder.instance(a => evaluatedSchema.encoder(a))
      def decoder: Decoder[A] = Decoder.instance(c => evaluatedSchema.decoder(c))
    }
  }

  def emptyRecord: Record[Unit] =
    Record(
      io.circe.Encoder.encodeUnit,
      io.circe.Decoder.decodeJsonObject.map(_ => ())
    )

  def field[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[A] =
    Record(
      io.circe.ObjectEncoder.instance[A](a => JsonObject.singleton(name, tpe.encoder.apply(a))),
      io.circe.Decoder.instance[A](cursor => tpe.decoder.tryDecode(cursor.downField(name)))
    )

  // FIXME Check that this is the correct way to model optional fields with circe
  def optField[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    Record(
      io.circe.ObjectEncoder.instance[Option[A]](maybeA => JsonObject.fromIterable(maybeA.map(a => name -> tpe.encoder.apply(a)))),
      io.circe.Decoder.instance[Option[A]](cursor => io.circe.Decoder.decodeOption(tpe.decoder).tryDecode(cursor.downField(name)))
    )

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged[A] {
      def taggedEncoded(a: A) = (tag, recordA.encoder.encodeObject(a))
      def taggedDecoder(tagName: String) = if (tag == tagName) Some(recordA.decoder) else None
    }

  def withDiscriminator[A](tagged: Tagged[A], discriminatorName: String): Tagged[A] =
    new Tagged[A] {
      override def discriminator: String = discriminatorName
      def taggedEncoded(a: A): (String, JsonObject) = tagged.taggedEncoded(a)
      def taggedDecoder(tag: String): Option[Decoder[A]] = tagged.taggedDecoder(tag)
    }

  def choiceTagged[A, B](taggedA: Tagged[A], taggedB: Tagged[B]): Tagged[Either[A, B]] =
    new Tagged[Either[A, B]] {
      def taggedEncoded(aOrB: Either[A, B]) = aOrB match {
        case Left(a)  => taggedA.taggedEncoded(a)
        case Right(b) => taggedB.taggedEncoded(b)
      }
      def taggedDecoder(tag: String) =
        taggedA.taggedDecoder(tag).map(_.map[Either[A, B]](Left(_)))
          .orElse(taggedB.taggedDecoder(tag).map(_.map[Either[A, B]](Right(_))))
    }

  def zipRecords[A, B](recordA: Record[A], recordB: Record[B]): Record[(A, B)] = {
    val encoder =
      io.circe.ObjectEncoder.instance[(A, B)] { case (a, b) =>
        // For some reason, `deepMerge` puts the fields of its left-hand-side *after*
        // the fields of its right-hand-side. Hence the inversion between `recordA`
        // and `recordB`.
        recordB.encoder.apply(b).deepMerge(recordA.encoder.apply(a)).asObject.get
      }
    val decoder = new io.circe.Decoder[(A, B)] {
      def apply(c: HCursor) = recordA.decoder.product(recordB.decoder).apply(c)
    }
    Record(encoder, decoder)
  }

  def xmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] =
    Record(record.encoder.contramapObject(g), record.decoder.map(f))

  def xmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] =
    new Tagged[B] {
      def taggedEncoded(b: B): (String, JsonObject) = tagged.taggedEncoded(g(b))
      def taggedDecoder(tag: String): Option[Decoder[B]] = tagged.taggedDecoder(tag).map(_.map(f))
    }

  def xmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
    JsonSchema(jsonSchema.encoder.contramap(g), jsonSchema.decoder.map(f))

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
      io.circe.Encoder.encodeIterable[A, C](jsonSchema.encoder, implicitly),
      io.circe.Decoder.decodeIterable[A, C](jsonSchema.decoder, factory)
    )

  implicit def mapJsonSchema[A](implicit jsonSchema: JsonSchema[A]): JsonSchema[Map[String, A]] =
    JsonSchema(
      io.circe.Encoder.encodeMap[String, A](implicitly, jsonSchema.encoder),
      io.circe.Decoder.decodeMap[String, A](implicitly, jsonSchema.decoder)
    )

}
