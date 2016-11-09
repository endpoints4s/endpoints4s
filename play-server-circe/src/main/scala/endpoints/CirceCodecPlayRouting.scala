package endpoints

import io.circe.jawn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Results}
import PlayCirce.circeJsonWriteable

/**
  * Implements [[CirceCodecAlg]] for [[EndpointPlayRouting]].
  */
trait CirceCodecPlayRouting extends EndpointPlayRouting with CirceCodecAlg {

  /** Decodes request entities by using the supplied codec */
  def jsonRequest[A : CirceCodec] =
    BodyParsers.parse.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .flatMap(CirceCodec[A].decoder.decodeJson).toEither
        .left.map(error => Results.BadRequest)
    }

  /** Builds a successful response (status = 200) having a JSON entity encoded using the supplied codec */
  def jsonResponse[A : CirceCodec] = a => Results.Ok(CirceCodec[A].encoder(a))

}
