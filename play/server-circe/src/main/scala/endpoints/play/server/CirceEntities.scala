package endpoints.play.server

import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import io.circe.jawn
import play.api.mvc.Results
import Util.circeJsonWriteable

/**
  * Implements [[algebra.CirceEntities]] for [[Endpoints]].
  */
trait CirceEntities extends Endpoints with algebra.CirceEntities {

  import playComponents.executionContext

  /** Decodes request entities by using the supplied codec */
  def jsonRequest[A : CirceCodec] =
    playComponents.playBodyParsers.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .right
        .flatMap(CirceCodec[A].decoder.decodeJson)
        .left.map(error => Results.BadRequest)
    }

  /** Builds a successful response (status = 200) having a JSON entity encoded using the supplied codec */
  def jsonResponse[A : CirceCodec] = a => Results.Ok(CirceCodec[A].encoder(a))

}
