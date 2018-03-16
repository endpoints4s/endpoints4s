package endpoints.algebra.circe

import io.circe.{Decoder => CirceDecoder, Encoder => CirceEncoder}

/**
  * Combines both an [[io.circe.Encoder]] and a [[io.circe.Decoder]] into a single type class.
  *
  * You don’t need to define instances by yourself as they can be derived from an existing pair
  * of an [[io.circe.Encoder]] and a [[io.circe.Decoder]].
  *
  * @see https://github.com/travisbrown/circe/issues/301
  */
trait CirceCodec[A] {
  def encoder: CirceEncoder[A]

  def decoder: CirceDecoder[A]
}

object CirceCodec {

  @inline def apply[A](implicit codec: CirceCodec[A]): CirceCodec[A] = codec

  implicit def fromEncoderAndDecoder[A](implicit enc: CirceEncoder[A], dec: CirceDecoder[A]): CirceCodec[A] =
    new CirceCodec[A] {
      val decoder = dec
      val encoder = enc
    }

}
