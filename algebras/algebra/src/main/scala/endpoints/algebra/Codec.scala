package endpoints.algebra

import endpoints.{PartialInvariantFunctor, PartialInvariantFunctorSyntax, Validated}

/**
  * A way to decode a `From` value into a `To` value.
  */
trait Decoder[-From, +To] {

  /**
    * @return The decoded `To` value, or the validation errors in case of failure.
    */
  def decode(from: From): Validated[To]
}

object Decoder {

  /** Combines two decoders, sequentially, by feeding the input of the second one with
    * the output of the first one
    */
  def sequentially[A, B, C](
      ab: Decoder[A, B]
  )(bc: Decoder[B, C]): Decoder[A, C] =
    from => ab.decode(from).flatMap(bc.decode)

  /** Produce a decoder that just outputs its input value */
  def identity[A]: Decoder[A, A] = identity[A]
}

/**
  * A way to encode a `From` value into a `To` value
  */
trait Encoder[-From, +To] {
  def encode(from: From): To
}

object Encoder {

  /** Combines two encoders, sequentially, by feeding the input of the second one with
    * the output of the first one
    */
  def sequentially[A, B, C](
      ab: Encoder[A, B]
  )(bc: Encoder[B, C]): Encoder[A, C] =
    from => bc.encode(ab.encode(from))

  /** Produce an encoder that just outputs its input value */
  def identity[A]: Encoder[A, A] = identity[A]
}

/**
  * A way to encode and decode values
  * @tparam E Type of encoded values
  * @tparam D Type of decoded values
  */
trait Codec[E, D] extends Decoder[E, D] with Encoder[D, E]

object Codec {

  def fromEncoderAndDecoder[E, D](
      encoder: Encoder[D, E]
  )(decoder: Decoder[E, D]): Codec[E, D] = new Codec[E, D] {
    def decode(from: E): Validated[D] = decoder.decode(from)
    def encode(from: D): E = encoder.encode(from)
  }

  /** Combines two codecs, sequentially, by feeding the input of the second one with
    * the output of the first one
    */
  def sequentially[A, B, C](ab: Codec[A, B])(bc: Codec[B, C]): Codec[A, C] =
    Codec.fromEncoderAndDecoder(Encoder.sequentially(bc)(ab))(
      Decoder.sequentially(ab)(bc)
    )

  /** Produce a codec that passes values through without touching them */
  def identity[A]: Codec[A, A] =
    fromEncoderAndDecoder[A, A](Encoder.identity)(Decoder.identity)
}

trait PartialInvariantFunctorCodecSyntax extends PartialInvariantFunctorSyntax {
  implicit class PartialInvariantFunctorCodecSyntax[A, F[_]](val fa: F[A])(
      implicit ev: PartialInvariantFunctor[F]
  ) {

    /**
      * Transforms an `F[A]` value into an `F[B]` value given a `Codec[A, B]`.
      *
      * This is useful to ''refine'' the type `A` into a possibly smaller type `B`.
      */
     def xmapPartialAlong[B](codec: Codec[A, B]): F[B] =
       ev.xmapPartial[A, B](fa, codec.decode, codec.encode)
  }
}
