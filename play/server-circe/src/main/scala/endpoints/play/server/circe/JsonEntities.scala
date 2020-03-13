package endpoints.play.server.circe

import endpoints.{Invalid, algebra}
import endpoints.play.server.EndpointsWithCustomErrors
import io.circe.{
  DecodingFailure,
  Json,
  ParsingFailure,
  parser,
  Decoder => CirceDecoder,
  Encoder => CirceEncoder
}
import Util.circeJsonWriteable
import cats.Show
import play.api.http.Writeable

/**
  * Interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Encoder]] to build JSON entities
  * in HTTP responses.
  *
  * @group interpreters
  */
trait JsonEntities extends EndpointsWithCustomErrors with algebra.JsonEntities {

  import playComponents.executionContext

  /** Decode requests using circe’s [[io.circe.Decoder]] */
  type JsonRequest[A] = CirceDecoder[A]

  /** Encode responses using circe’s [[io.circe.Encoder]] */
  type JsonResponse[A] = CirceEncoder[A]

  def jsonRequest[A: CirceDecoder]: RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { text =>
      parser
        .parse(text)
        .left
        .map(Show[ParsingFailure].show)
        .flatMap { json =>
          CirceDecoder[A].decodeJson(json).left.map(Show[DecodingFailure].show)
        }
        .left
        .map(error => handleClientErrors(Invalid(error)))
    }

  def jsonResponse[A: CirceEncoder]: ResponseEntity[A] =
    responseEntityFromWriteable(
      implicitly[Writeable[Json]].map(CirceEncoder[A].apply(_))
    )

}
