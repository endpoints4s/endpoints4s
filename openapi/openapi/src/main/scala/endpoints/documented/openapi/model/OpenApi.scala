package endpoints.documented.openapi.model

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
    Json.fromFields(mediaTypes.map { case (tpe, mediaType) =>
      tpe -> Json.obj("schema" -> mediaType.schema.fold[Json](Json.obj())(_.asJson))
    })

}

sealed trait Schema
object Schema {
  case class Object(properties: List[Property], description: Option[String]) extends Schema
  case class Array(elementType: Schema) extends Schema
  case class Property(name: String, schema: Schema, isRequired: Boolean)
  case class Primitive(name: String) extends Schema
  case class OneOf(alternatives: List[Schema], description: Option[String]) extends Schema

  implicit val jsonEncoder: ObjectEncoder[Schema] =
    ObjectEncoder.instance {
      case Primitive(name) => JsonObject.singleton("type", Json.fromString(name))
      case Array(elementType) =>
        JsonObject.fromIterable(
          "type" -> Json.fromString("array") ::
          "items" -> jsonEncoder.apply(elementType) ::
          Nil
        )
      case Object(properties, description) =>
        val fields =
          "type" -> Json.fromString("object") ::
          "properties" -> Json.fromFields(
            properties.map { property =>
              property.name -> jsonEncoder.apply(property.schema)
            }
          ) ::
          Nil
        val fieldsWithDescription =
          description.fold(fields)(s => "description" -> Json.fromString(s) :: fields)
        val requiredProperties = properties.filter(_.isRequired)
        val fieldsWithRequired =
          if (requiredProperties.isEmpty) fieldsWithDescription
          else "required" -> Json.arr(requiredProperties.map(p => Json.fromString(p.name)): _*) :: fieldsWithDescription
        JsonObject.fromIterable(fieldsWithRequired)
      case OneOf(alternatives, description) =>
        val fields =
          "type" -> Json.fromString("object") ::
          "oneOf" -> Json.fromValues(alternatives.map(jsonEncoder.apply)) ::
          Nil
        val fieldsWithDescription =
          description.fold(fields)(s => "description" -> Json.fromString(s) :: fields)
        JsonObject.fromIterable(fieldsWithDescription)
    }

}
