package endpoints4s.algebra.circe

import endpoints4s.algebra
import io.circe.{Decoder, Encoder, Json}

trait ChunkedJsonEntitiesTestApi
    extends algebra.ChunkedJsonEntitiesTestApi
    with JsonEntitiesFromCodecs {

  implicit lazy val counterEncoder: Encoder[Counter] =
    Encoder.instance(counter => Json.obj("value" -> Json.fromInt(counter.value)))
  implicit lazy val counterDecoder: Decoder[Counter] =
    Decoder.instance(cursor => cursor.get[Int]("value")).map(Counter(_))

  def counterCodec: JsonCodec[Counter] = implicitly

}
