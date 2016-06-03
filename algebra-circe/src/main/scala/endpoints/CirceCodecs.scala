package endpoints

import io.circe.{Decoder, Encoder}

trait CirceCodecs extends JsonEntitiesAlg {

  type JsonResponse[A] = CirceCodec[A]
  type JsonRequest[A] = CirceCodec[A]

}

trait CirceCodec[A] {
  def encoder: Encoder[A]
  def decoder: Decoder[A]
}

object CirceCodec {

  @inline def apply[A](implicit codec: CirceCodec[A]): CirceCodec[A] = codec

  implicit def fromEncoderAndDecoder[A](implicit enc: Encoder[A], dec: Decoder[A]): CirceCodec[A] =
    new CirceCodec[A] {
      val decoder = dec
      val encoder = enc
    }

}