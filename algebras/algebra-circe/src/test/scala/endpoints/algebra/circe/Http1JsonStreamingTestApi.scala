package endpoints.algebra.circe

import endpoints.algebra
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

trait Http1JsonStreamingTestApi
  extends algebra.Http1JsonStreamingTestApi
    with JsonEntitiesFromCodec {

  implicit lazy val counterEncoder: Encoder[Counter] = deriveEncoder
  implicit lazy val counterDecoder: Decoder[Counter] = deriveDecoder

  def counterCodec: JsonCodec[Counter] = implicitly

}
