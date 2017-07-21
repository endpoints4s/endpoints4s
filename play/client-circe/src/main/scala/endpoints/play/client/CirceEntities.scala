package endpoints.play.client

import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import endpoints.play
import io.circe.jawn
import endpoints.play.PlayCirce.circeJsonWriteable

/**
  * Implements [[algebra.CirceEntities]] for [[play.client.Endpoints]]
  */
trait CirceEntities extends algebra.CirceEntities { this: Endpoints =>

  /** Builds a request entity by using the supplied codec */
  def jsonRequest[A : CirceCodec]: RequestEntity[A] = {
    case (a, wsRequest) => wsRequest.withBody(CirceCodec[A].encoder.apply(a))
  }

  /** Decodes a response entity by using the supplied codec */
  def jsonResponse[A : CirceCodec]: Response[A] =
    response => jawn.parse(response.body).right.flatMap(CirceCodec[A].decoder.decodeJson)

}
