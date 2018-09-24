package endpoints.play.server.playjson

import endpoints.algebra
import endpoints.algebra.Documentation
import endpoints.play.server.Endpoints
import play.api.libs.json.Json
import play.api.mvc.Results

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that uses Play JSON [[play.api.libs.json.Reads]] to decode
  * JSON entities in HTTP requests, and [[play.api.libs.json.Writes]] to build JSON entities in HTTP responses.
  */
trait JsonSchemaEntities extends Endpoints with algebra.JsonSchemaEntities with endpoints.playjson.JsonSchemas {

  import playComponents.executionContext

  def jsonRequest[A: JsonSchema](docs: Documentation): RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { text =>
      Json.parse(text).validate(implicitly[JsonSchema[A]].reads).asEither
        .left.map(ignoredError => Results.BadRequest)
    }

  def jsonResponse[A: JsonSchema](docs: Documentation): Response[A] =
    a => Results.Ok(implicitly[JsonSchema[A]].writes.writes(a))

}
