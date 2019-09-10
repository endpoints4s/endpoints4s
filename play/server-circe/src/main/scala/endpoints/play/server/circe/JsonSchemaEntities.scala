package endpoints.play.server.circe

import endpoints.algebra
import endpoints.play.server.Endpoints
import endpoints.play.server.circe.Util.circeJsonWriteable
import io.circe.{Json, parser}
import play.api.http.Writeable
import play.api.mvc.Results

/**
  * Interpreter for [[algebra.JsonEntities]] that uses circe’s [[io.circe.Decoder]] to decode
  * JSON entities in HTTP requests, and circe’s [[io.circe.Encoder]] to build JSON entities
  * in HTTP responses.
  */
trait JsonSchemaEntities extends Endpoints with algebra.JsonSchemaEntities with endpoints.circe.JsonSchemas {

  import playComponents.executionContext


  def jsonRequest[A : JsonSchema]: RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { text =>
      parser.parse(text)
        .right.flatMap(implicitly[JsonSchema[A]].decoder.decodeJson)
        .left.map(ignoredError => Results.BadRequest)
    }

  def jsonResponse[A : JsonSchema]: ResponseEntity[A] =
    implicitly[Writeable[Json]].map(a => implicitly[JsonSchema[A]].encoder(a))

}
