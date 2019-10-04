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

/**
  * A way to encode a `From` value into a `To` value
  */
trait Encoder[-From, +To] {
  def encode(from: From): To
}

/**
  * A way to encode and decode values
  * @tparam E Type of encoded values
  * @tparam D Type of decoded values
  */
trait Codec[E, D] extends Decoder[E, D] with Encoder[D, E]
