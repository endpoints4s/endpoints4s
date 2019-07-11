package endpoints.algebra

import scala.util.{Failure, Success, Try}

trait Decoder[-From, +To] {
  def decode(from: From): Either[Exception, To] // TODO Make the error type more useful
  def decodeToTry(from: From): Try[To] = decode(from).fold[Try[To]](Failure(_), Success(_))
}

trait Encoder[-From, +To] {
  def encode(from: From): To
}

trait Codec[E, D] extends Decoder[E, D] with Encoder[D, E]
