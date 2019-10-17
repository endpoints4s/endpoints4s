package endpoints.play.server.circe

import cats.Show
import endpoints.{Invalid, algebra}
import endpoints.play.server.EndpointsWithCustomErrors
import endpoints.play.server.circe.Util.circeJsonWriteable
import io.circe.{DecodingFailure, Json, ParsingFailure, parser}
import play.api.http.Writeable

/**
  * Interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Encoder]] to build JSON entities
  * in HTTP responses.
  */
trait JsonSchemaEntities extends EndpointsWithCustomErrors with algebra.JsonSchemaEntities with endpoints.circe.JsonSchemas {

  import playComponents.executionContext

  def jsonRequest[A : JsonSchema]: RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { text =>
      parser.parse(text).left.map(Show[ParsingFailure].show)
        .right.flatMap { json =>
          implicitly[JsonSchema[A]].decoder.decodeJson(json).left.map(Show[DecodingFailure].show)
        }
        .left.map(error => handleClientErrors(Invalid(error)))
    }

  def jsonResponse[A : JsonSchema]: ResponseEntity[A] =
    implicitly[Writeable[Json]].map(a => implicitly[JsonSchema[A]].encoder(a))

}
