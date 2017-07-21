package endpoints.algebra

trait Decoder[-From, +To] {
  def decode(from: From): Either[Throwable, To] // TODO Make the error type more useful
}

trait Encoder[-From, +To] {
  def encode(from: From): To
}
