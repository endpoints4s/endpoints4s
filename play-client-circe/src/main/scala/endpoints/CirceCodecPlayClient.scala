package endpoints

import io.circe.jawn
import PlayCirce.circeJsonWriteable

/**
  * Implements [[CirceCodecAlg]] for [[EndpointPlayClient]]
  */
trait CirceCodecPlayClient extends CirceCodecAlg { this: EndpointPlayClient =>

  /** Builds a request entity by using the supplied codec */
  def jsonRequest[A : CirceCodec]: RequestEntity[A] = {
    case (a, wsRequest) => wsRequest.post(CirceCodec[A].encoder.apply(a))
  }

  /** Decodes a response entity by using the supplied codec */
  def jsonResponse[A : CirceCodec]: Response[A] =
    response => jawn.parse(response.body).right.flatMap(CirceCodec[A].decoder.decodeJson)

}
