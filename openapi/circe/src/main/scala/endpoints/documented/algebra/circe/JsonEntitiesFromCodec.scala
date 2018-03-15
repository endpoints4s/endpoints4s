package endpoints.documented.algebra.circe

import endpoints.algebra.Codec
import endpoints.algebra.circe.CirceCodec
import io.circe.parser

/**
  * Partial interpreter for [[endpoints.documented.algebra.JsonEntitiesFromCodec]] that only
  * fixed the `JsonCodec[A]` type to a [[CirceCodec]].
  *
  * The `jsonRequest` and `jsonResponse` operations have to be implemented by
  * a more specialized interpreter.
  *
  */
trait JsonEntitiesFromCodec extends endpoints.documented.algebra.JsonEntitiesFromCodec {

  type JsonCodec[A] = CirceCodec[A]

  implicit def jsonCodec[A](implicit codec: CirceCodec[A]): Codec[String, A] = new Codec[String, A] {

    def decode(from: String): Either[Exception, A] =
      parser.parse(from).right.flatMap(codec.decoder.decodeJson)

    def encode(from: A): String =
      codec.encoder.apply(from).noSpaces

  }

}
