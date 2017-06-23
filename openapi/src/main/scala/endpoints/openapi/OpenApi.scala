package endpoints.openapi

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

      def jsonMediaTypes(mediaTypes: Map[String, MediaType]): Json =
        Json.fromFields(mediaTypes.map { case (tpe, mediaType) => tpe -> Json.obj() /* TODO */ })

      JsonObject.fromMap(Map(
        "openapi" -> Json.fromString("3.0.0"),
        "info" -> Json.obj(
          "title" -> Json.fromString(openApi.info.title),
          "version" -> Json.fromString(openApi.info.version)
        ),
        "paths" -> Json.fromFields(
          openApi.paths.to[List].map { case (path, item) =>
            val itemObj =
              Json.fromFields(
                item.operations.to[List].map { case (verb, op) =>
                  val fields =
                    (
                      "parameters" -> Json.fromValues(
                        op.parameters.map { parameter =>
                          val fields =
                            "name" -> Json.fromString(parameter.name) ::
                            "in" -> Json.fromString(parameter.in match {
                              case In.Cookie => "cookie"
                              case In.Header => "header"
                              case In.Path   => "path"
                              case In.Query  => "query"
                            }) ::
                            Nil
                          Json.fromFields(
                            if (parameter.required) "required" -> Json.fromBoolean(true) :: fields
                            else fields
                          )
                        }
                      )
                    ) ::
                    (
                      "responses" -> Json.fromFields(
                        op.responses.to[List].map { case (status, resp) =>
                          status.toString -> Json.fromFields(
                            "description" -> Json.fromString(resp.description) ::
                            (if (resp.content.nonEmpty) {
                              "content" -> jsonMediaTypes(resp.content) ::
                              Nil
                            } else Nil)
                          )
                        }
                      )
                    ) ::
                    Nil

                  val fieldsWithRequestEntity =
                    op.requestBody.map { requestBody =>
                      "requestBody" -> Json.fromFields({
                        val requiredFields =
                          "content" -> jsonMediaTypes(requestBody.content) ::
                          Nil
                        requestBody.description.fold(requiredFields)(d => "description" -> Json.fromString(d) :: requiredFields)
                      })
                    }.fold(fields)(_ :: fields)

                  verb -> Json.fromFields(fieldsWithRequestEntity)
                }
              )
            (path, itemObj)
          }
        )
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

case class Operation(
  parameters: List[Parameter],
  requestBody: Option[RequestBody],
  responses: Map[Int, Response]
)

case class RequestBody(
  description: Option[String],
  content: Map[String, MediaType]
) {
  assert(content.nonEmpty)
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

sealed trait In
object In {
  case object Query extends In
  case object Path extends In
  case object Header extends In
  case object Cookie extends In
}

case class MediaType(schema: Option[Schema])

sealed trait Schema
