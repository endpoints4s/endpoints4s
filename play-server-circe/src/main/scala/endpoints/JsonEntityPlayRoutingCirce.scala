package endpoints

import io.circe.{Decoder, Encoder, jawn}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{BodyParsers, Results}
import PlayCirce.circeJsonWriteable

/**
  * Interpreter for [[JsonEntityAlg]] that uses circe’s [[Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[Encoder]] to build JSON entities
  * in HTTP responses.
  */
trait JsonEntityPlayRoutingCirce extends EndpointPlayRouting with JsonEntityAlg {

  /** Decode requests using circe’s [[Decoder]] */
  type JsonRequest[A] = Decoder[A]

  /** Encode responses using circe’s [[Encoder]] */
  type JsonResponse[A] = Encoder[A]

  def jsonRequest[A : Decoder] =
    BodyParsers.parse.raw.validate { buffer =>
      jawn.parseFile(buffer.asFile)
        .flatMap(Decoder[A].decodeJson).toEither
        .left.map(error => Results.BadRequest)
    }

  def jsonResponse[A : Encoder] = a => Results.Ok(Encoder[A].apply(a))

}
