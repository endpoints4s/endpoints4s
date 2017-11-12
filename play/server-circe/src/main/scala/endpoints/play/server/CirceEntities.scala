package endpoints.play.server

import endpoints.algebra
import endpoints.algebra.CirceEntities.CirceCodec
import io.circe.jawn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Results}
import Util.circeJsonWriteable

/**
  * Implements [[algebra.CirceEntities]] for [[Endpoints]].
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
