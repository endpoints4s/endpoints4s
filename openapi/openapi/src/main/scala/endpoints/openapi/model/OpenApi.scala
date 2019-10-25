package endpoints.openapi.model

/**
  * @see [[https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md]]
  */
case class OpenApi(
  info: Info,
  paths: Map[String, PathItem],
  components: Components
)

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

  case class Array(elementType: Schema, description: Option[String]) extends Schema

  case class Enum(elementType: Schema, values: List[String], description: Option[String]) extends Schema

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
