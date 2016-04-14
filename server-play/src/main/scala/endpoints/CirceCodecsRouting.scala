package endpoints

import io.circe.jawn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Results}

trait CirceCodecsRouting extends PlayRouting with CirceCodecs {

  def jsonRequest[A : CirceCodec] =
    BodyParsers.parse.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .flatMap(CirceCodec[A].decoder.decodeJson).toEither
        .left.map(error => Results.BadRequest)
    }

  def jsonResponse[A : CirceCodec] = a => Results.Ok(CirceCodec[A].encoder(a))

}
