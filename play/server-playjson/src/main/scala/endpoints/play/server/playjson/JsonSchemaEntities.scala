package endpoints.play.server.playjson

import endpoints.{Invalid, algebra}
import endpoints.play.server.Endpoints
import play.api.http.Writeable
import play.api.libs.json.{JsPath, JsValue, Json, JsonValidationError}

import scala.util.Try

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that uses Play JSON [[play.api.libs.json.Reads]] to decode
  * JSON entities in HTTP requests, and [[play.api.libs.json.Writes]] to build JSON entities in HTTP responses.
  */
trait JsonSchemaEntities extends Endpoints with algebra.JsonSchemaEntities with endpoints.playjson.JsonSchemas {

  import playComponents.executionContext

  def jsonRequest[A: JsonSchema]: RequestEntity[A] =
    playComponents.playBodyParsers.tolerantText.validate { text =>
      Try(Json.parse(text)).toEither.left.map(_ => Invalid("Unable to parse entity as JSON"))
        .right.flatMap { json =>
          def showErrors(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): Invalid =
            Invalid((
              for {
                (path, pathErrors) <- errors.iterator
                error <- pathErrors
              } yield s"${error.message} for ${path.toJsonString}"
            ).toSeq)
          json.validate(implicitly[JsonSchema[A]].reads).asEither.left.map(showErrors)
        }
        .left.map(handleClientErrors)
    }

  def jsonResponse[A: JsonSchema]: ResponseEntity[A] =
    implicitly[Writeable[JsValue]].map(a => implicitly[JsonSchema[A]].writes.writes(a))

}
