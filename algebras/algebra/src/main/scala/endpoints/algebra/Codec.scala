package endpoints.algebra

import endpoints.Validated

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

  /** Transform the decoded type of the given `decoder` with the function `f`. */
  def mapDecoded[From, To1, To2](decoder: Decoder[From, To1])(f: To1 => To2): Decoder[From, To2] =
    from => decoder.decode(from).map(f)

  /** Transform the decoded type of the given `decoder` with the partial function `f`. */
  def flatMapDecoded[From, To1, To2](decoder: Decoder[From, To1])(f: To1 => Validated[To2]): Decoder[From, To2] =
    from => decoder.decode(from).flatMap(f)

}

/**
  * A way to encode a `From` value into a `To` value
  */
trait Encoder[-From, +To] {
  def encode(from: From): To
}

object Encoder {

  /** Transform the decoded type of the given `encoder` with the function `f`. */
  def mapDecoded[From1, From2, To](encoder: Encoder[From1, To])(f: From2 => From1): Encoder[From2, To] =
    from => encoder.encode(f(from))

}

/**
  * A way to encode and decode values
  * @tparam E Type of encoded values
  * @tparam D Type of decoded values
  */
trait Codec[E, D] extends Decoder[E, D] with Encoder[D, E]
