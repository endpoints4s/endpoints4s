package endpoints

import io.circe.jawn
import PlayCirce.circeJsonWriteable

trait PlayClientCirce extends CirceCodecs { this: PlayClient =>

  def jsonRequest[A : CirceCodec]: RequestEntity[A] = {
    case (a, wsRequest) => wsRequest.post(CirceCodec[A].encoder.apply(a))
  }

  def jsonResponse[A : CirceCodec]: Response[A] =
    response => jawn.parse(response.body).flatMap(CirceCodec[A].decoder.decodeJson)

}
