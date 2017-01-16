package endpoints.play.routing

import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import endpoints.play
import io.circe.jawn
import _root_.play.api.libs.concurrent.Execution.Implicits.defaultContext
import _root_.play.api.mvc.{BodyParsers, Results}
import endpoints.PlayCirce.circeJsonWriteable

/**
  * Implements [[algebra.CirceEntities]] for [[play.routing.Endpoints]].
  */
trait CirceEntities extends Endpoints with algebra.CirceEntities {

  /** Decodes request entities by using the supplied codec */
  def jsonRequest[A : CirceCodec] =
    BodyParsers.parse.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .right
        .flatMap(CirceCodec[A].decoder.decodeJson)
        .left.map(error => Results.BadRequest)
    }

  /** Builds a successful response (status = 200) having a JSON entity encoded using the supplied codec */
  def jsonResponse[A : CirceCodec] = a => Results.Ok(CirceCodec[A].encoder(a))

}
