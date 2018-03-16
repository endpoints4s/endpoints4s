package endpoints.algebra

trait Decoder[-From, +To] {
  def decode(from: From): Either[Exception, To] // TODO Make the error type more useful
}

trait Encoder[-From, +To] {
  def encode(from: From): To
}

trait Codec[E, D] extends Decoder[E, D] with Encoder[D, E]
