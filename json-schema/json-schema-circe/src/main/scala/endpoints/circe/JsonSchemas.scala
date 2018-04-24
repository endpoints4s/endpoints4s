package endpoints
package circe

import endpoints.algebra
import endpoints.algebra.circe.CirceCodec
import io.circe._

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

/**
  * An interpreter for [[endpoints.algebra.JsonSchemas]] that produces a circe codec.
  */
trait  JsonSchemas
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

  }

  type Record[A] = JsonSchema[A]

  trait Tagged[A] extends Record[A] {
    def taggedEncoded(a: A): (String, Json)
    def taggedDecoder(tag: String): Option[Decoder[A]]
    def encoder: Encoder[A] =
      Encoder.instance { a =>
        val (tag, json) = taggedEncoded(a)
        Json.obj(tag -> json)
      }
    def decoder: Decoder[A] =
      Decoder.instance { cursor =>
        cursor.as[JsonObject].right.flatMap { jsonObject =>
          jsonObject.toList.headOption match {
            case Some((tag, json)) =>
              taggedDecoder(tag) match {
                case Some(dec) => dec.decodeJson(json)
                case None => Left(DecodingFailure(s"No decoder for type tag $tag", Nil))
              }
            case None => Left(DecodingFailure("Missing type tag field", Nil))
          }
        }
      }
  }

  def field[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[A] =
    JsonSchema(
      io.circe.Encoder.instance[A](a => Json.obj(name -> tpe.encoder.apply(a))),
      io.circe.Decoder.instance[A](cursor => tpe.decoder.tryDecode(cursor.downField(name)))
    )

  // FIXME Check that this is the correct way to model optional fields with circe
  def optField[A](name: String, documentation: Option[String] = None)(implicit tpe: JsonSchema[A]): Record[Option[A]] =
    JsonSchema(
      io.circe.Encoder.instance[Option[A]](a => Json.obj(name -> io.circe.Encoder.encodeOption(tpe.encoder).apply(a))),
      io.circe.Decoder.instance[Option[A]](cursor => io.circe.Decoder.decodeOption(tpe.decoder).tryDecode(cursor.downField(name)))
    )

  def taggedRecord[A](recordA: Record[A], tag: String): Tagged[A] =
    new Tagged[A] {
      def taggedEncoded(a: A) = (tag, recordA.encoder.apply(a))
      def taggedDecoder(tagName: String) = if (tag == tagName) Some(recordA.decoder) else None
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
      io.circe.Encoder.instance[(A, B)] { case (a, b) =>
        recordA.encoder.apply(a).deepMerge(recordB.encoder.apply(b))
      }
    val decoder = new io.circe.Decoder[(A, B)] {
      def apply(c: HCursor) = recordA.decoder.product(recordB.decoder).apply(c)
    }
    JsonSchema(encoder, decoder)
  }

  def invmapRecord[A, B](record: Record[A], f: A => B, g: B => A): Record[B] = invmapJsonSchema(record, f, g)

  def invmapTagged[A, B](tagged: Tagged[A], f: A => B, g: B => A): Tagged[B] =
    new Tagged[B] {
      def taggedEncoded(b: B): (String, Json) = tagged.taggedEncoded(g(b))
      def taggedDecoder(tag: String): Option[Decoder[B]] = tagged.taggedDecoder(tag).map(_.map(f))
    }

  def invmapJsonSchema[A, B](jsonSchema: JsonSchema[A], f: A => B, g: B => A): JsonSchema[B] =
    JsonSchema(jsonSchema.encoder.contramap(g), jsonSchema.decoder.map(f))

  implicit def stringJsonSchema: JsonSchema[String] = JsonSchema(implicitly, implicitly)

  implicit def intJsonSchema: JsonSchema[Int] = JsonSchema(implicitly, implicitly)

  implicit def longJsonSchema: JsonSchema[Long] = JsonSchema(implicitly, implicitly)

  implicit def bigdecimalJsonSchema: JsonSchema[BigDecimal] = JsonSchema(implicitly, implicitly)

  implicit def doubleJsonSchema: JsonSchema[Double] = JsonSchema(implicitly, implicitly)

  implicit def booleanJsonSchema: JsonSchema[Boolean] = JsonSchema(implicitly, implicitly)

  implicit def arrayJsonSchema[C[X] <: Seq[X], A](implicit jsonSchema: JsonSchema[A], cbf: CanBuildFrom[_, A, C[A]]): JsonSchema[C[A]] =
    JsonSchema(
      io.circe.Encoder.encodeList[A](jsonSchema.encoder).contramap[C[A]](_.toList),
      io.circe.Decoder.decodeList[A](jsonSchema.decoder).map(_.to[C])
    )

}
