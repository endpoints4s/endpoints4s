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
      JsonObject.fromMap(Map(
        "openapi" -> Json.fromString("3.0.0"),
        "info" -> Json.obj(
          "title" -> Json.fromString(openApi.info.title),
          "version" -> Json.fromString(openApi.info.version)
        ),
        "paths" -> Json.obj(
          openApi.paths.to[List].map { case (path, item) =>
            val itemObj =
              Json.obj(
                item.operations.to[List].map { case (name, op) =>
                  name -> Json.obj(
                    "parameters" -> Json.arr(
                      op.parameters.map { parameter =>
                        val fields = Seq(
                          "name" -> Json.fromString(parameter.name),
                          "in" -> Json.fromString(parameter.in match {
                            case In.Cookie => "cookie"
                            case In.Header => "header"
                            case In.Path   => "path"
                            case In.Query  => "query"
                          })
                        )
                        Json.obj(
                          (
                            if (parameter.required) fields :+ ("required" -> Json.fromBoolean(true))
                            else fields
                          ): _*
                        )
                      }: _*
                    ),
                    "responses" -> Json.obj(
                      op.responses.to[List].map { case (status, resp) =>
                        status.toString -> Json.obj(
                          "description" -> Json.fromString(resp.description)
                        )
                      }: _*
                    )
                  )
                }: _*
              )
            (path, itemObj)
          }: _*
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

// TODO requestBody
case class Operation(
  parameters: List[Parameter],
  responses: Map[Int, Response]
)

case class Response(
  description: String
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
