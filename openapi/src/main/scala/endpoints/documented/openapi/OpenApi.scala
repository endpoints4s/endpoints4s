package endpoints.documented.openapi

import io.circe.syntax._
import io.circe.{Json, JsonObject, ObjectEncoder}

/**
  * @see [[https://github.com/OAI/OpenAPI-Specification/blob/OpenAPI.next/versions/3.0.md]]
  */
case class OpenApi(
  info: Info,
  paths: Map[String, PathItem]
)

object OpenApi {

  implicit val jsonEncoder: ObjectEncoder[OpenApi] =
    ObjectEncoder.instance { openApi =>
      JsonObject.fromMap(Map(
        "openapi" -> Json.fromString("3.0.0"),
        "info" -> Json.obj(
          "title" -> Json.fromString(openApi.info.title),
          "version" -> Json.fromString(openApi.info.version)
        ),
        "paths" -> Json.fromFields(openApi.paths.to[List].map { case (path, item) => (path, item.asJson) })
      ))
    }

}

case class Info(
  title: String,
  version: String
)

case class PathItem(
  operations: Map[String, Operation]
)

object PathItem {

  implicit val jsonEncoder: ObjectEncoder[PathItem] =
    ObjectEncoder.instance { item =>
      JsonObject.fromIterable(item.operations.to[List].map { case (verb, op) => (verb, op.asJson) })
    }

}

case class Operation(
  parameters: List[Parameter],
  requestBody: Option[RequestBody],
  responses: Map[Int, Response]
)

object Operation {

  implicit val jsonEncoder: ObjectEncoder[Operation] =
    ObjectEncoder.instance { op =>
      val fields =
        "parameters" -> Json.fromValues(op.parameters.map(_.asJson)) ::
        (
          "responses" -> Json.fromFields(
            op.responses.to[List].map { case (status, resp) =>
              status.toString -> Json.fromFields(
                "description" -> Json.fromString(resp.description) ::
                (if (resp.content.nonEmpty) {
                  "content" -> MediaType.jsonMediaTypes(resp.content) ::
                    Nil
                } else Nil)
              )
            }
          )
        ) ::
        Nil

      val fieldsWithRequestEntity =
        op.requestBody.map { requestBody =>
          "requestBody" -> requestBody.asJson
        }.fold(fields)(_ :: fields)

      JsonObject.fromIterable(fieldsWithRequestEntity)
    }

}

case class RequestBody(
  description: Option[String],
  content: Map[String, MediaType]
) {
  assert(content.nonEmpty)
}

object RequestBody {

  implicit val jsonEncoder: ObjectEncoder[RequestBody] =
    ObjectEncoder.instance { requestBody =>
      JsonObject.fromIterable({
        val requiredFields =
          "content" -> MediaType.jsonMediaTypes(requestBody.content) ::
          Nil
        requestBody.description.fold(requiredFields)(d => "description" -> Json.fromString(d) :: requiredFields)
      })
    }

}

case class Response(
  description: String,
  content: Map[String, MediaType]
)

case class Parameter(
  name: String,
  in: In,
  required: Boolean
)

object Parameter {

  implicit val jsonEncoder: ObjectEncoder[Parameter] =
    ObjectEncoder.instance { parameter =>
      val fields =
        "name" -> Json.fromString(parameter.name) ::
        "in" -> Json.fromString(parameter.in match {
          case In.Cookie => "cookie"
          case In.Header => "header"
          case In.Path => "path"
          case In.Query => "query"
        }) ::
        Nil
      JsonObject.fromIterable(
        if (parameter.required) "required" -> Json.fromBoolean(true) :: fields
        else fields
      )
    }

}

sealed trait In
object In {
  case object Query extends In
  case object Path extends In
  case object Header extends In
  case object Cookie extends In
}

case class MediaType(schema: Option[Schema])

object MediaType {

  def jsonMediaTypes(mediaTypes: Map[String, MediaType]): Json =
    Json.fromFields(mediaTypes.map { case (tpe, mediaType) => tpe -> Json.obj() /* TODO Document the media schema */ })

}

sealed trait Schema
