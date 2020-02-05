package endpoints.algebra.circe

import endpoints.algebra
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

trait ChunkedJsonEntitiesTestApi
  extends algebra.ChunkedJsonEntitiesTestApi
    with JsonEntitiesFromCodecs {

  implicit lazy val counterEncoder: Encoder[Counter] = deriveEncoder
  implicit lazy val counterDecoder: Decoder[Counter] = deriveDecoder

  def counterCodec: JsonCodec[Counter] = implicitly

}