package endpoints.algebra

import java.util.UUID
import endpoints.{Valid, Invalid, Validated}

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

  /** Produce a codec to/from a string. If the parsing function fails, the
    * decoding output is an [[Invalid]] with a message mentioning the type name.
    *
    * @param type name of the type being decoded
    * @param parse parsing function to use, with exceptions turned into [[Invalid]]
    * @param print printing function to use, not supposed to throw exceptions
    */
  def parseStringCatchingExceptions[A](
      `type`: String,
      parse: String => A,
      print: A => String = (x: A) => x.toString()
  ): Codec[String, A] = new Codec[String, A] {
    def encode(x: A): String = print(x)
    def decode(str: String): Validated[A] =
      try { Valid(parse(str)) }
      catch { case _: Throwable => Invalid(s"Invalid ${`type`} value '$str'") }
  }

  val uuidCodec: Codec[String, UUID] =
    parseStringCatchingExceptions("UUID", UUID.fromString)

  val intCodec: Codec[String, Int] =
    parseStringCatchingExceptions("integer", _.toInt)

  val longCodec: Codec[String, Long] =
    parseStringCatchingExceptions("integer", _.toLong)

  val doubleCodec: Codec[String, Double] =
    parseStringCatchingExceptions("number", _.toDouble)

  val booleanCodec: Codec[String, Boolean] =
    parseStringCatchingExceptions("boolean", {
      case "true" | "1"  => true
      case "false" | "0" => false
    })
}
