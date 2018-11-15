package endpoints.openapi.model

import io.circe.syntax._
import io.circe.{Json, JsonObject, ObjectEncoder}

/**
  * @see [[https://github.com/OAI/OpenAPI-Specification/blob/OpenAPI.next/versions/3.0.md]]
  */
case class OpenApi(
  info: Info,
  paths: Map[String, PathItem],
  components: Components
)

object OpenApi {

  implicit val jsonEncoder: ObjectEncoder[OpenApi] =
    ObjectEncoder.instance { openApi =>
      val mandatoryFields =
        "openapi" -> Json.fromString("3.0.0") ::
        "info" -> Json.obj(
          "title" -> Json.fromString(openApi.info.title),
          "version" -> Json.fromString(openApi.info.version)
        ) ::
        "paths" -> Json.fromFields(openApi.paths.to[List].map { case (path, item) => (path, item.asJson) }) ::
        Nil
      val fields =
        if (openApi.components.schemas.isEmpty) mandatoryFields
        else ("components" -> openApi.components.asJson) :: mandatoryFields
      JsonObject.fromIterable(fields)
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
      JsonObject.fromIterable(item.operations.map { case (verb, op) => (verb, op.asJson) })
    }

}

case class Components(schemas: Map[String, Schema])

object Components {

  implicit val jsonEncoder: ObjectEncoder[Components] = {
    ObjectEncoder.instance { components =>
      val schemas = components.schemas.mapValues(_.asJson).toSeq.sortBy(_._1)
      JsonObject.singleton("schemas", JsonObject.fromIterable(schemas).asJson)
    }
  }
}

case class Operation(
  summary: Option[String],
  description: Option[String],
  parameters: List[Parameter],
  requestBody: Option[RequestBody],
  responses: Map[Int, Response],
  tags: List[String]
)

object Operation {

  implicit val jsonEncoder: ObjectEncoder[Operation] =
    ObjectEncoder.instance { op =>
      val optFields = List(
        op.summary.map(x => "summary" -> x.asJson),
        op.description.map(x => "description" -> x.asJson),
        op.requestBody.map(x => "requestBody" -> x.asJson),
        op.tags.headOption.map(_ => "tags" -> op.tags.asJson),
        if (op.parameters.isEmpty) None
        else Some("parameters" -> Json.fromValues(op.parameters.map(_.asJson)))
      ).flatten
      val fields =
        (
          "responses" -> Json.fromJsonObject(JsonObject.fromIterable(
            op.responses.map { case (status, resp) =>
              status.toString -> Json.fromFields(
                "description" -> Json.fromString(resp.description) ::
                  (if (resp.content.nonEmpty) {
                    "content" -> MediaType.jsonMediaTypes(resp.content) ::
                      Nil
                  } else Nil)
              )
            }
          ))
        ) ::
        optFields

      JsonObject.fromIterable(fields)
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
  required: Boolean,
  description: Option[String],
  schema: Schema // not specified in openapi spec but swagger-editor breaks without it for path parameters
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
          "schema" -> parameter.schema.asJson ::
          List(
            parameter.description.map(s => "description" -> Json.fromString(s))
          ).flatten
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
      tpe -> mediaType.schema.fold(Json.obj())(schema => Json.obj("schema" -> schema.asJson))
    })

}

sealed trait Schema

object Schema {

  case class Object(properties: List[Property], description: Option[String]) extends Schema

  case class Array(elementType: Schema) extends Schema

  case class Enum(elementType: Schema, values: Seq[String]) extends Schema

  case class Property(name: String, schema: Schema, isRequired: Boolean, description: Option[String])

  case class Primitive(name: String, format: Option[String]) extends Schema

  case class OneOf(discriminatorName: String, alternatives: List[(String, Schema)], description: Option[String]) extends Schema

  case class AllOf(schemas: List[Schema]) extends Schema

  case class Reference(name: String, original: Option[Schema]) extends Schema

  object Reference {
    def toRefPath(name: String): String =
      s"#/components/schemas/$name"
  }

  val simpleString = Primitive("string", None)
  val simpleInteger = Primitive("integer", None)

  implicit val jsonEncoder: ObjectEncoder[Schema] =
    ObjectEncoder.instance {
      case Primitive(name, None) =>
        JsonObject.singleton("type", Json.fromString(name))
      case Primitive(name, Some(format)) =>
        JsonObject.fromIterable(
          "type" -> Json.fromString(name) ::
            "format" -> Json.fromString(format) ::
            Nil
        )
      case Array(elementType) =>
        JsonObject.fromIterable(
          "type" -> Json.fromString("array") ::
            "items" -> jsonEncoder.apply(elementType) ::
            Nil
        )
      case Enum(elementType, values) =>
        jsonEncoder.encodeObject(elementType).add("enum", Json.fromValues(values.map(Json.fromString)))
      case Object(properties, description) =>
        val fields =
          "type" -> Json.fromString("object") ::
            "properties" -> Json.fromFields(
              properties.map { property =>
                val propertyFields =
                  property.description match {
                    case None => jsonEncoder.apply(property.schema)
                    case Some(s) => Json.fromFields(("description" -> Json.fromString(s)) +: jsonEncoder.encodeObject(property.schema).toVector)
                  }
                property.name -> propertyFields
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
      case OneOf(discriminatorName, alternatives, description) =>
        val mapping = alternatives.collect { case (tag, Schema.Reference(name, _)) =>
          tag -> Json.fromString(Reference.toRefPath(name))
        }
        val fields =
            "oneOf" -> Json.fromValues(alternatives.map(a => jsonEncoder(a._2))) ::
            "discriminator" -> Json.obj(
              "propertyName" -> Json.fromString(discriminatorName),
              "mapping" -> Json.fromFields(mapping)
            ) ::
            Nil
        val fieldsWithDescription =
          description.fold(fields)(s => "description" -> Json.fromString(s) :: fields)
        JsonObject.fromIterable(fieldsWithDescription)
      case AllOf(schemas) =>
        JsonObject.singleton("allOf", Json.fromValues(schemas.map(jsonEncoder.apply)))
      case Reference(name, _) =>
        JsonObject.singleton("$ref", Json.fromString(Reference.toRefPath(name)))
    }

}
