package endpoints.openapi.model

import endpoints.algebra.Encoder

import scala.collection.mutable

/**
  * @see [[https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md]]
  */
case class OpenApi(
  info: Info,
  paths: Map[String, PathItem],
  components: Components
)

object OpenApi {

  val openApiVersion = "3.0.0"

  private def mapJson[A](map: Map[String, A])(f: A => ujson.Value): ujson.Obj =
    new ujson.Obj(mutable.LinkedHashMap(map.iterator.map { case (k, v) => (k, f(v)) }.toSeq: _*))

  private def schemaJson(schema: Schema): ujson.Obj = {
    val fields = mutable.LinkedHashMap.empty[String, ujson.Value]
    schema match {
      case Schema.Primitive(name, format, _) =>
        fields += "type" -> ujson.Str(name)
        format.foreach(s => fields += "format" -> ujson.Str(s))
      case Schema.Object(properties, additionalProperties, _) =>
        fields ++= List(
          "type"       -> "object",
          "properties" -> mapJson(properties.iterator.map(p => p.name -> p.schema.withDefinedDescription(p.description)).toMap)(schemaJson)
        )
        val required = properties.filter(_.isRequired).map(_.name)
        if (required.nonEmpty) {
          fields += "required" -> ujson.Arr(required.map(ujson.Str(_)): _*)
        }
        additionalProperties.foreach(p => fields += "additionalProperties" -> schemaJson(p))
      case Schema.Array(elementType, _) =>
        val itemsSchema = elementType match {
          case Left(value)  => schemaJson(value)
          case Right(value) => ujson.Arr(value.map(schemaJson): _*)
        }
        fields ++= List(
          "type"  -> "array",
          "items" -> itemsSchema
        )
      case Schema.Enum(elementType, values, description) =>
        fields ++= schemaJson(elementType.withDefinedDescription(description)).value
        fields += "enum" -> ujson.Arr(values: _*)
      case Schema.OneOf(discriminatorName, alternatives, _) =>
        val mappingJson =
          new ujson.Obj(mutable.LinkedHashMap(alternatives.collect {
            case (tag, ref: Schema.Reference) => tag -> ujson.Str(Schema.Reference.toRefPath(ref.name))
          }: _*))
        fields ++= List(
          "oneOf"         -> ujson.Arr(alternatives.map(kv => schemaJson(kv._2)): _*),
          "discriminator" -> ujson.Obj(
            "propertyName" -> ujson.Str(discriminatorName),
            "mapping"      -> mappingJson
          )
        )
      case Schema.AllOf(schemas, _) =>
        fields += "allOf" -> ujson.Arr(schemas.map(schemaJson): _*)
      case Schema.Reference(name, _, _) =>
        fields += "$ref" -> ujson.Str(Schema.Reference.toRefPath(name))
    }
    for (description <- schema.description) {
      fields += "description" -> ujson.Str(description)
    }
    new ujson.Obj(fields)
  }

  private def securitySchemeJson(securityScheme: SecurityScheme): ujson.Obj = {
    val fields = mutable.LinkedHashMap[String, ujson.Value](
      "type" -> ujson.Str(securityScheme.`type`)
    )
    for (description <- securityScheme.description) {
      fields += "description" -> ujson.Str(description)
    }
    for (name <- securityScheme.name) {
      fields += "name" -> ujson.Str(name)
    }
    for (in <- securityScheme.in) {
      fields += "in" -> ujson.Str(in)
    }
    for (scheme <- securityScheme.scheme) {
      fields += "scheme" -> ujson.Str(scheme)
    }
    for (bearerFormat <- securityScheme.bearerFormat) {
      fields += "bearerFormat" -> ujson.Str(bearerFormat)
    }
    new ujson.Obj(fields)
  }

  private def componentsJson(components: Components): ujson.Obj =
    ujson.Obj(
      "schemas"         -> mapJson(components.schemas)(schemaJson),
      "securitySchemes" -> mapJson(components.securitySchemes)(securitySchemeJson)
    )

  private def responseJson(response: Response): ujson.Obj = {
    val fields = mutable.LinkedHashMap[String, ujson.Value](
      "description" -> ujson.Str(response.description)
    )
    if (response.content.nonEmpty) {
      fields += "content" -> mapJson(response.content)(mediaTypeJson)
    }
    new ujson.Obj(fields)
  }

  def mediaTypeJson(mediaType: MediaType): ujson.Value =
    mediaType.schema match {
      case Some(schema) => ujson.Obj("schema" -> schemaJson(schema))
      case None         => ujson.Obj()
    }

  private def operationJson(operation: Operation): ujson.Obj = {
    val fields = mutable.LinkedHashMap[String, ujson.Value](
      "responses" -> mapJson(operation.responses)(responseJson)
    )
    operation.summary.foreach { summary =>
      fields += "summary" -> ujson.Str(summary)
    }
    if (operation.parameters.nonEmpty) {
      fields += "parameters" -> ujson.Arr(operation.parameters.map(parameterJson): _*)
    }
    operation.requestBody.foreach { requestBody =>
      fields += "requestBody" -> requestBodyJson(requestBody)
    }
    if (operation.tags.nonEmpty) {
      fields += "tags" -> ujson.Arr(operation.tags.map(ujson.Str): _*)
    }
    if (operation.security.nonEmpty) {
      fields += "security" -> ujson.Arr(operation.security.map(securityRequirementJson): _*)
    }
    if (operation.callbacks.nonEmpty) {
      fields += "callbacks" -> mapJson(operation.callbacks)(pathsJson)
    }
    if (operation.deprecated) {
      fields += "deprecated" -> ujson.True
    }
    new ujson.Obj(fields)
  }

  private def parameterJson(parameter: Parameter): ujson.Value = {
    val fields = mutable.LinkedHashMap[String, ujson.Value](
      "name"   -> ujson.Str(parameter.name),
      "in"     -> inJson(parameter.in),
      "schema" -> schemaJson(parameter.schema)
    )
    parameter.description.foreach { description =>
      fields += "description" -> ujson.Str(description)
    }
    if (parameter.required) {
      fields += "required" -> ujson.True
    }
    new ujson.Obj(fields)
  }

  private def inJson(in: In): ujson.Value =
    in match {
      case In.Query  => ujson.Str("query")
      case In.Path   => ujson.Str("path")
      case In.Header => ujson.Str("header")
      case In.Cookie => ujson.Str("cookie")
    }

  private def requestBodyJson(body: RequestBody): ujson.Value = {
    val fields = mutable.LinkedHashMap[String, ujson.Value](
      "content" -> mapJson(body.content)(mediaTypeJson)
    )
    body.description.foreach { description =>
      fields += "description" -> ujson.Str(description)
    }
    new ujson.Obj(fields)
  }

  private def securityRequirementJson(securityRequirement: SecurityRequirement): ujson.Value =
    ujson.Obj(
      securityRequirement.name -> ujson.Arr(securityRequirement.scopes.map(ujson.Str): _*)
    )

  private def pathsJson(paths: Map[String, PathItem]): ujson.Obj =
    mapJson(paths)(pathItem =>
      mapJson(pathItem.operations)(operationJson)
    )

  private val jsonEncoder: Encoder[OpenApi, ujson.Value] =
    openApi => {
      val fields: mutable.LinkedHashMap[String, ujson.Value] = mutable.LinkedHashMap(
        "openapi" -> ujson.Str(openApiVersion),
        "info"    -> ujson.Obj(
          "title"   -> ujson.Str(openApi.info.title),
          "version" -> ujson.Str(openApi.info.version)
        ),
        "paths"   -> pathsJson(openApi.paths)
      )
      if (openApi.components.schemas.nonEmpty || openApi.components.securitySchemes.nonEmpty) {
        fields += "components" -> componentsJson(openApi.components)
      }
      new ujson.Obj(fields)
    }

  implicit val stringEncoder: Encoder[OpenApi, String] =
    openApi => jsonEncoder.encode(openApi).transform(ujson.StringRenderer()).toString

}

case class Info(
  title: String,
  version: String
)

case class PathItem(
  operations: Map[String, Operation]
)

case class Components(schemas: Map[String, Schema],
                      securitySchemes: Map[String, SecurityScheme])

case class Operation(
  summary: Option[String],
  description: Option[String],
  parameters: List[Parameter],
  requestBody: Option[RequestBody],
  responses: Map[String, Response],
  tags: List[String],
  security: List[SecurityRequirement],
  callbacks: Map[String, Map[String, PathItem]],
  deprecated: Boolean
)

case class SecurityRequirement(name: String,
                               scheme: SecurityScheme,
                               scopes: List[String] = Nil)

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
  required: Boolean,
  description: Option[String],
  schema: Schema // not specified in openapi spec but swagger-editor breaks without it for path parameters
)

sealed trait In

object In {
  case object Query extends In
  case object Path extends In
  case object Header extends In
  case object Cookie extends In

  // All the possible values.
  val values: Seq[In] = Query :: Path :: Header :: Cookie :: Nil
}

case class MediaType(schema: Option[Schema])

sealed trait Schema {
  def description: Option[String]

  /**
    * @return The same schema with its description overridden by the given `description`,
    *         or stay unchanged if this one is empty.
    */
  def withDefinedDescription(description: Option[String]): Schema = this match {
    case s: Schema.Object    => s.copy(description = description.orElse(s.description))
    case s: Schema.Array     => s.copy(description = description.orElse(s.description))
    case s: Schema.Enum      => s.copy(description = description.orElse(s.description))
    case s: Schema.Primitive => s.copy(description = description.orElse(s.description))
    case s: Schema.OneOf     => s.copy(description = description.orElse(s.description))
    case s: Schema.AllOf     => s.copy(description = description.orElse(s.description))
    case s: Schema.Reference => s.copy(description = description.orElse(s.description))
  }
}

object Schema {

  case class Object(properties: List[Property], additionalProperties: Option[Schema], description: Option[String]) extends Schema

  case class Array(elementType: Either[Schema, List[Schema]], description: Option[String]) extends Schema

  case class Enum(elementType: Schema, values: List[ujson.Value], description: Option[String]) extends Schema

  case class Property(name: String, schema: Schema, isRequired: Boolean, description: Option[String])

  case class Primitive(name: String, format: Option[String], description: Option[String]) extends Schema

  case class OneOf(discriminatorName: String, alternatives: List[(String, Schema)], description: Option[String]) extends Schema

  case class AllOf(schemas: List[Schema], description: Option[String]) extends Schema

  case class Reference(name: String, original: Option[Schema], description: Option[String]) extends Schema

  object Reference {
    def toRefPath(name: String): String =
      s"#/components/schemas/$name"
  }

  val simpleUUID = Primitive("string", format = Some("uuid"), None)
  val simpleString = Primitive("string", None, None)
  val simpleInteger = Primitive("integer", None, None)
  val simpleBoolean = Primitive("boolean", None, None)
  val simpleNumber = Primitive("number", None, None)

}

case class SecurityScheme(`type`: String, // TODO This should be a sealed trait, the `type` field should only exist in the JSON representation
                          description: Option[String],
                          name: Option[String],
                          in: Option[String], // TODO Create a typed enumeration
                          scheme: Option[String],
                          bearerFormat: Option[String])

object SecurityScheme {

  def httpBasic: SecurityScheme = SecurityScheme(
    `type` = "http",
    description = Some("Http Basic Authentication"),
    name = None,
    in = None,
    scheme = Some("basic"),
    bearerFormat = None
  )
}
