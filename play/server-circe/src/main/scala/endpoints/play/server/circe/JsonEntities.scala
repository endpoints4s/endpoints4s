package endpoints.play.server.circe

import endpoints.algebra
import endpoints.play.server.Endpoints
import io.circe.{parser, Decoder => CirceDecoder, Encoder => CirceEncoder}
import play.api.mvc.Results
import Util.circeJsonWriteable

/**
  * Interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Encoder]] to build JSON entities
  * in HTTP responses.
  */
trait JsonEntities extends Endpoints with algebra.JsonEntities {

  import playComponents.executionContext

  /** Decode requests using circe’s [[io.circe.Decoder]] */
  type JsonRequest[A] = CirceDecoder[A]

  /** Encode responses using circe’s [[io.circe.Encoder]] */
  type JsonResponse[A] = CirceEncoder[A]

  def jsonRequest[A : CirceDecoder]: RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { text =>
      parser.parse(text)
        .right.flatMap(CirceDecoder[A].decodeJson)
        .left.map(ignoredError => Results.BadRequest)
    }

  def jsonResponse[A : CirceEncoder]: Response[A] = a => Results.Ok(CirceEncoder[A].apply(a))

}
