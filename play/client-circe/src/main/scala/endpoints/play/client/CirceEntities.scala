package endpoints.play.client

import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import io.circe.{Json, jawn}
import play.api.libs.ws.{BodyWritable, InMemoryBody}
import play.api.mvc.Codec
import play.api.http.ContentTypes
import endpoints.play

/**
  * Implements [[algebra.CirceEntities]] for [[play.client.Endpoints]]
  */
trait CirceEntities extends algebra.CirceEntities { this: Endpoints =>

  implicit def circeJsonBodyWriteable(implicit codec: Codec): BodyWritable[Json] =
    new BodyWritable[Json](json => InMemoryBody(codec.encode(json.noSpaces)), ContentTypes.JSON)

  /** Builds a request entity by using the supplied codec */
  def jsonRequest[A : CirceCodec]: RequestEntity[A] = {
    case (a, wsRequest) => wsRequest.withBody(CirceCodec[A].encoder.apply(a))
  }

  /** Decodes a response entity by using the supplied codec */
  def jsonResponse[A : CirceCodec]: Response[A] =
    response => jawn.parse(response.body).right.flatMap(CirceCodec[A].decoder.decodeJson)

}
