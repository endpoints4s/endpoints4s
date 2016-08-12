package endpoints

import io.circe.{Decoder, Encoder, jawn}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Results}
import PlayCirce.circeJsonWriteable

trait JsonEntityPlayRoutingCirce extends EndpointPlayRouting with JsonEntityAlg {

  type JsonRequest[A] = Decoder[A]

  type JsonResponse[A] = Encoder[A]

  def jsonRequest[A : Decoder] =
    BodyParsers.parse.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .flatMap(Decoder[A].decodeJson).toEither
        .left.map(error => Results.BadRequest)
    }

  def jsonResponse[A : Encoder] = a => Results.Ok(Encoder[A].apply(a))

}
