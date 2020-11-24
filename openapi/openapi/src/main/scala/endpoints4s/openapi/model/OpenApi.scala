package endpoints4s.openapi.model

import java.io.Serializable

import endpoints4s.{Encoder, Hashing}
import endpoints4s.algebra.{ExternalDocumentationObject, Tag}

import scala.collection.mutable

/** @see [[https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md]]
  * @note Throws an exception on creation if several tags have the same name but not the same other attributes.
  */
@throws(classOf[IllegalArgumentException])
final class OpenApi private (
    val info: Info,
    val paths: Map[String, PathItem],
    val components: Components
) extends Serializable {

  override def toString =
    s"OpenApi($info, $paths, $components)"

  override def equals(other: Any): Boolean =
    other match {
      case that: OpenApi =>
        info == that.info && paths == that.paths && components == that.components
      case _ => false
    }

  override def hashCode(): Int = Hashing.hash(info, paths, components)

  val tags: Set[Tag] = OpenApi.extractTags(paths)

  private[this] def copy(
      info: Info = info,
      paths: Map[String, PathItem] = paths,
      components: Components = components
  ): OpenApi =
    new OpenApi(info, paths, components)

  def withInfo(info: Info): OpenApi =
    copy(info = info)

  def withPaths(paths: Map[String, PathItem]): OpenApi =
    copy(paths = paths)

  def withComponents(components: Components): OpenApi =
    copy(components = components)
}

object OpenApi {

  val openApiVersion = "3.0.0"

  def apply(info: Info, paths: Map[String, PathItem], components: Components) =
    new OpenApi(info, paths, components)

  private def mapJson[A](map: Map[String, A])(f: A => ujson.Value): ujson.Obj =
    new ujson.Obj(mutable.LinkedHashMap(map.iterator.map { case (k, v) =>
      (k, f(v))
    }.toSeq: _*))

  private[openapi] def schemaJson(schema: Schema): ujson.Obj = {
    val fields = mutable.LinkedHashMap.empty[String, ujson.Value]
    schema match {
      case primitive: Schema.Primitive =>
        fields += "type" -> ujson.Str(primitive.name)
        primitive.format.foreach(s => fields += "format" -> ujson.Str(s))
        primitive.minimum.foreach(d => fields += "minimum" -> ujson.Num(d))
        primitive.exclusiveMinimum.foreach(b => fields += "exclusiveMinimum" -> ujson.Bool(b))
        primitive.maximum.foreach(d => fields += "maximum" -> ujson.Num(d))
        primitive.exclusiveMaximum.foreach(b => fields += "exclusiveMaximum" -> ujson.Bool(b))
        primitive.multipleOf.foreach(d => fields += "multipleOf" -> ujson.Num(d))
      case obj: Schema.Object =>
        fields ++= List(
          "type" -> "object",
          "properties" -> new ujson.Obj(
            mutable.LinkedHashMap(
              obj.properties.map(p =>
                p.name -> schemaJson(
                  p.schema.withDefinedDescription(p.description)
                )
              ): _*
            )
          )
        )
        val required = obj.properties.filter(_.isRequired).map(_.name)
        if (required.nonEmpty) {
          fields += "required" -> ujson.Arr(required.map(ujson.Str(_)): _*)
        }
        obj.additionalProperties.foreach(p => fields += "additionalProperties" -> schemaJson(p))
      case array: Schema.Array =>
        val itemsSchema = array.elementType match {
          case Left(value)  => schemaJson(value)
          case Right(value) => ujson.Arr(value.map(schemaJson): _*)
        }
        fields ++= List(
          "type" -> "array",
          "items" -> itemsSchema
        )
      case enm: Schema.Enum =>
        fields ++= schemaJson(
          enm.elementType.withDefinedDescription(enm.description)
        ).value
        fields += "enum" -> ujson.Arr(enm.values: _*)
      case oneOf: Schema.OneOf =>
        fields ++=
          (oneOf.alternatives match {
            case discAlternatives: Schema.DiscriminatedAlternatives =>
              val mappingFields: mutable.LinkedHashMap[String, ujson.Value] =
                mutable.LinkedHashMap(discAlternatives.alternatives.collect {
                  case (tag, ref: Schema.Reference) =>
                    tag -> ujson.Str(Schema.Reference.toRefPath(ref.name))
                }: _*)
              val discFields = mutable.LinkedHashMap.empty[String, ujson.Value]
              discFields += "propertyName" -> ujson.Str(
                discAlternatives.discriminatorFieldName
              )
              if (mappingFields.nonEmpty) {
                discFields += "mapping" -> new ujson.Obj(mappingFields)
              }
              List(
                "oneOf" -> ujson
                  .Arr(
                    discAlternatives.alternatives
                      .map(kv => schemaJson(kv._2)): _*
                  ),
                "discriminator" -> ujson.Obj(discFields)
              )
            case enumAlternatives: Schema.EnumeratedAlternatives =>
              List(
                "oneOf" -> ujson
                  .Arr(enumAlternatives.alternatives.map(schemaJson): _*)
              )
          })
      case allOf: Schema.AllOf =>
        fields += "allOf" -> ujson.Arr(allOf.schemas.map(schemaJson): _*)
      case reference: Schema.Reference =>
        fields += "$ref" -> ujson.Str(
          Schema.Reference.toRefPath(reference.name)
        )
    }
    for (description <- schema.description) {
      fields += "description" -> ujson.Str(description)
    }
    for (example <- schema.example) {
      fields += "example" -> example
    }
    for (title <- schema.title) {
      fields += "title" -> title
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

  private def infoJson(info: Info): ujson.Obj = {
    val fields: mutable.LinkedHashMap[String, ujson.Value] =
      mutable.LinkedHashMap(
        "title" -> ujson.Str(info.title),
        "version" -> ujson.Str(info.version)
      )
    info.description.foreach(description => fields += "description" -> ujson.Str(description))
    ujson.Obj(fields)
  }

  private def componentsJson(components: Components): ujson.Obj =
    ujson.Obj(
      "schemas" -> mapJson(components.schemas)(schemaJson),
      "securitySchemes" -> mapJson(components.securitySchemes)(
        securitySchemeJson
      )
    )

  private def responseJson(response: Response): ujson.Obj = {
    val fields = mutable.LinkedHashMap[String, ujson.Value](
      "description" -> ujson.Str(response.description)
    )
    if (response.headers.nonEmpty) {
      fields += "headers" -> mapJson(response.headers)(responseHeaderJson)
    }
    if (response.content.nonEmpty) {
      fields += "content" -> mapJson(response.content)(mediaTypeJson)
    }
    new ujson.Obj(fields)
  }

  def responseHeaderJson(responseHeader: ResponseHeader): ujson.Value = {
    val fields = mutable.LinkedHashMap[String, ujson.Value](
      "schema" -> schemaJson(responseHeader.schema)
    )
    if (responseHeader.required) {
      fields += "required" -> ujson.True
    }
    responseHeader.description.foreach { description =>
      fields += "description" -> ujson.Str(description)
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
    operation.operationId.foreach { id =>
      fields += "operationId" -> ujson.Str(id)
    }
    operation.summary.foreach { summary =>
      fields += "summary" -> ujson.Str(summary)
    }
    operation.description.foreach { description =>
      fields += "description" -> ujson.Str(description)
    }
    if (operation.parameters.nonEmpty) {
      fields += "parameters" -> ujson.Arr(
        operation.parameters.map(parameterJson): _*
      )
    }
    operation.requestBody.foreach { requestBody =>
      fields += "requestBody" -> requestBodyJson(requestBody)
    }
    if (operation.tags.nonEmpty) {
      fields += "tags" -> ujson.Arr(
        operation.tags.map(tag => ujson.Str(tag.name)): _*
      )
    }
    if (operation.security.nonEmpty) {
      fields += "security" -> ujson.Arr(
        operation.security.map(securityRequirementJson): _*
      )
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
      "name" -> ujson.Str(parameter.name),
      "in" -> inJson(parameter.in),
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

  private def tagJson(tag: Tag): ujson.Value = {
    val fields: mutable.LinkedHashMap[String, ujson.Value] =
      mutable.LinkedHashMap(
        "name" -> ujson.Str(tag.name)
      )

    if (tag.description.nonEmpty) {
      fields += "description" -> tag.description.get
    }
    if (tag.externalDocs.nonEmpty) {
      fields += "externalDocs" -> externalDocumentationObjectJson(
        tag.externalDocs.get
      )
    }
    new ujson.Obj(fields)
  }

  private def externalDocumentationObjectJson(
      externalDoc: ExternalDocumentationObject
  ): ujson.Value = {
    val fields: mutable.LinkedHashMap[String, ujson.Value] =
      mutable.LinkedHashMap(
        "url" -> ujson.Str(externalDoc.url)
      )

    if (externalDoc.description.nonEmpty)
      fields += "description" -> externalDoc.description.get
    new ujson.Obj(fields)
  }

  private def securityRequirementJson(
      securityRequirement: SecurityRequirement
  ): ujson.Value =
    ujson.Obj(
      securityRequirement.name -> ujson.Arr(
        securityRequirement.scopes.map(ujson.Str): _*
      )
    )

  private def pathsJson(paths: Map[String, PathItem]): ujson.Obj =
    mapJson(paths)(pathItem => mapJson(pathItem.operations)(operationJson))

  private val jsonEncoder: Encoder[OpenApi, ujson.Value] =
    openApi => {
      val fields: mutable.LinkedHashMap[String, ujson.Value] =
        mutable.LinkedHashMap(
          "openapi" -> ujson.Str(openApiVersion),
          "info" -> infoJson(openApi.info),
          "paths" -> pathsJson(openApi.paths)
        )
      if (openApi.tags.nonEmpty) {
        val tagsAsJson = openApi.tags.map(tag => tagJson(tag)).toList
        fields += "tags" -> ujson.Arr(tagsAsJson: _*)
      }
      if (openApi.components.schemas.nonEmpty || openApi.components.securitySchemes.nonEmpty) {
        fields += "components" -> componentsJson(openApi.components)
      }
      new ujson.Obj(fields)
    }

  private def extractTags(paths: Map[String, PathItem]): Set[Tag] = {
    val allTags = paths.flatMap { case (_, pathItem) =>
      pathItem.operations.map { case (_, operation) =>
        operation.tags
      }
    }.flatten

    val tagsByName = allTags.groupBy(_.name)
    tagsByName.foreach { case (_, listOfTags) =>
      val set = listOfTags.toSet
      if (set.size > 1) {
        throw new IllegalArgumentException(
          s"Found tags with the same name but different values: $set"
        )
      }
    }

    // Note that tags without any additional information will still be shown. However there is no
    // reason to add these tags to the root since tags with only names can and will be defined at
    // the moment they will be used in the endpoint descriptions themselves.
    allTags
      .filter(tag => tag.description.nonEmpty || tag.externalDocs.nonEmpty)
      .toSet
  }

  implicit val stringEncoder: Encoder[OpenApi, String] =
    openApi => jsonEncoder.encode(openApi).transform(ujson.StringRenderer()).toString

}

final class Info private (
    val title: String,
    val version: String,
    val description: Option[String]
) extends Serializable {

  override def toString: String =
    s"Info($title, $version, $description)"

  override def equals(other: Any): Boolean =
    other match {
      case that: Info =>
        title == that.title && version == that.version && description == that.description
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(title, version, description)

  private[this] def copy(
      title: String = title,
      version: String = version,
      description: Option[String] = description
  ): Info =
    new Info(title, version, description)

  def withTitle(title: String): Info =
    copy(title = title)

  def withVersion(version: String): Info =
    copy(version = version)

  def withDescription(description: Option[String]): Info =
    copy(description = description)

}

object Info {

  def apply(title: String, version: String): Info =
    new Info(title, version, None)

}

final class PathItem private (
    val operations: Map[String, Operation]
) extends Serializable {

  override def toString =
    s"PathItem($operations)"

  override def equals(other: Any): Boolean =
    other match {
      case that: PathItem => operations == that.operations
      case _              => false
    }

  override def hashCode(): Int =
    Hashing.hash(operations)

  def withOperations(operations: Map[String, Operation]): PathItem =
    PathItem(operations)

}

object PathItem {

  def apply(operations: Map[String, Operation]): PathItem =
    new PathItem(operations)

}

final class Components private (
    val schemas: Map[String, Schema],
    val securitySchemes: Map[String, SecurityScheme]
) extends Serializable {

  override def toString: String =
    s"Components($schemas, $securitySchemes)"

  override def equals(other: Any): Boolean =
    other match {
      case that: Components =>
        schemas == that.schemas && securitySchemes == that.securitySchemes
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(schemas, securitySchemes)

  private[this] def copy(
      schemas: Map[String, Schema] = schemas,
      securitySchemes: Map[String, SecurityScheme] = securitySchemes
  ) = new Components(schemas, securitySchemes)

  def withSchemas(schemas: Map[String, Schema]): Components =
    copy(schemas = schemas)

  def withSecuritySchemas(
      securitySchemes: Map[String, SecurityScheme]
  ): Components =
    copy(securitySchemes = securitySchemes)

}

object Components {

  def apply(
      schemas: Map[String, Schema],
      securitySchemes: Map[String, SecurityScheme]
  ): Components =
    new Components(schemas, securitySchemes)

}

final class Operation private (
    val operationId: Option[String],
    val summary: Option[String],
    val description: Option[String],
    val parameters: List[Parameter],
    val requestBody: Option[RequestBody],
    val responses: Map[String, Response],
    val tags: List[Tag],
    val security: List[SecurityRequirement],
    val callbacks: Map[String, Map[String, PathItem]],
    val deprecated: Boolean
) extends Serializable {

  override def toString: String =
    s"Operation($operationId, $summary, $description, $parameters, $requestBody, $responses, $tags, $security, $callbacks, $deprecated)"

  override def equals(other: Any): Boolean =
    other match {
      case that: Operation =>
        operationId == that.operationId && summary == that.summary && description == that.description && parameters == that.parameters &&
          requestBody == that.requestBody && responses == that.responses && tags == that.tags &&
          security == that.security && callbacks == that.callbacks && deprecated == that.deprecated
    }

  override def hashCode(): Int =
    Hashing.hash(
      operationId,
      summary,
      description,
      parameters,
      requestBody,
      responses,
      tags,
      security,
      callbacks,
      deprecated
    )

  private[this] def copy(
      id: Option[String] = operationId,
      summary: Option[String] = summary,
      description: Option[String] = description,
      parameters: List[Parameter] = parameters,
      requestBody: Option[RequestBody] = requestBody,
      responses: Map[String, Response] = responses,
      tags: List[Tag] = tags,
      security: List[SecurityRequirement] = security,
      callbacks: Map[String, Map[String, PathItem]] = callbacks,
      deprecated: Boolean = deprecated
  ): Operation =
    Operation(
      id,
      summary,
      description,
      parameters,
      requestBody,
      responses,
      tags,
      security,
      callbacks,
      deprecated
    )

  def withOperationId(operationId: Option[String]): Operation =
    copy(id = operationId)

  def withSummary(summary: Option[String]): Operation =
    copy(summary = summary)

  def withDescription(description: Option[String]): Operation =
    copy(description = description)

  def withParameters(parameters: List[Parameter]): Operation =
    copy(parameters = parameters)

  def withRequestBody(requestBody: Option[RequestBody]): Operation =
    copy(requestBody = requestBody)

  def withResponses(responses: Map[String, Response]): Operation =
    copy(responses = responses)

  def withTags(tags: List[Tag]): Operation =
    copy(tags = tags)

  def withSecurity(security: List[SecurityRequirement]): Operation =
    copy(security = security)

  def withCallbacks(callbacks: Map[String, Map[String, PathItem]]): Operation =
    copy(callbacks = callbacks)

  def withDeprecated(deprecated: Boolean): Operation =
    copy(deprecated = deprecated)
}

object Operation {

  def apply(
      id: Option[String],
      summary: Option[String],
      description: Option[String],
      parameters: List[Parameter],
      requestBody: Option[RequestBody],
      responses: Map[String, Response],
      tags: List[Tag],
      security: List[SecurityRequirement],
      callbacks: Map[String, Map[String, PathItem]],
      deprecated: Boolean
  ): Operation =
    new Operation(
      id,
      summary,
      description,
      parameters,
      requestBody,
      responses,
      tags,
      security,
      callbacks,
      deprecated
    )

}

final class SecurityRequirement private (
    val name: String,
    val scheme: SecurityScheme,
    val scopes: List[String]
) extends Serializable {

  override def toString: String =
    s"SecurityRequirement($name, $scheme, $scopes)"

  override def equals(other: Any): Boolean =
    other match {
      case that: SecurityRequirement =>
        name == that.name && scheme == that.scheme && scopes == that.scopes
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(name, scheme, scopes)

  private[this] def copy(
      name: String = name,
      scheme: SecurityScheme = scheme,
      scopes: List[String] = scopes
  ): SecurityRequirement =
    new SecurityRequirement(name, scheme, scopes)

  def withName(name: String): SecurityRequirement =
    copy(name = name)

  def withScheme(scheme: SecurityScheme): SecurityRequirement =
    copy(scheme = scheme)

  def withScopes(scopes: List[String]): SecurityRequirement =
    copy(scopes = scopes)
}

object SecurityRequirement {

  def apply(
      name: String,
      scheme: SecurityScheme
  ): SecurityRequirement =
    new SecurityRequirement(name, scheme, Nil)

  @deprecated(
    "Use `SecurityRequirement().withScopes(...)` instead of `SecurityRequirement(scopes = ...)`",
    "1.0.0"
  )
  def apply(
      name: String,
      scheme: SecurityScheme,
      scopes: List[String] = Nil
  ): SecurityRequirement =
    new SecurityRequirement(name, scheme, scopes)
}

final class RequestBody private (
    val description: Option[String],
    val content: Map[String, MediaType]
) extends Serializable {
  assert(content.nonEmpty)

  override def toString: String =
    s"RequestBody($description, $content)"

  override def equals(other: Any): Boolean =
    other match {
      case that: RequestBody =>
        description == that.description && content == that.content
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(description, content)

  private[this] def copy(
      description: Option[String] = description,
      content: Map[String, MediaType] = content
  ): RequestBody =
    new RequestBody(description, content)

  def withDescription(description: Option[String]): RequestBody =
    copy(description = description)

  def withContent(content: Map[String, MediaType]): RequestBody =
    copy(content = content)
}

object RequestBody {

  def apply(description: Option[String], content: Map[String, MediaType]) =
    new RequestBody(description, content)

}

final class Response private (
    val description: String,
    val headers: Map[String, ResponseHeader],
    val content: Map[String, MediaType]
) extends Serializable {

  override def toString: String =
    s"Response($description, $headers, $content)"

  override def equals(other: Any): Boolean =
    other match {
      case that: Response =>
        description == that.description && headers == that.headers && content == that.content
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(description, headers, content)

  private[this] def copy(
      description: String = description,
      headers: Map[String, ResponseHeader] = headers,
      content: Map[String, MediaType] = content
  ): Response =
    new Response(description, headers, content)

  def withDescription(description: String): Response =
    copy(description = description)

  def withHeaders(headers: Map[String, ResponseHeader]): Response =
    copy(headers = headers)

  def withContent(content: Map[String, MediaType]): Response =
    copy(content = content)
}

object Response {

  def apply(
      description: String,
      headers: Map[String, ResponseHeader],
      content: Map[String, MediaType]
  ): Response =
    new Response(description, headers, content)

}

// Note: request headers don’t need a dedicated class because they are modeled as `Parameter`s
final class ResponseHeader private (
    val required: Boolean,
    val description: Option[String],
    val schema: Schema
) extends Serializable {

  override def toString: String =
    s"ResponseHeader($required, $description, $schema)"

  override def equals(other: Any): Boolean =
    other match {
      case that: ResponseHeader =>
        required == that.required && description == that.description && schema == that.schema
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(required, description, schema)

  private def copy(
      required: Boolean = required,
      description: Option[String] = description,
      schema: Schema = schema
  ): ResponseHeader =
    new ResponseHeader(required, description, schema)

  def withRequired(required: Boolean): ResponseHeader =
    copy(required = required)

  def withDescription(description: Option[String]): ResponseHeader =
    copy(description = description)

  def withSchema(schema: Schema): ResponseHeader =
    copy(schema = schema)

}

object ResponseHeader {

  def apply(
      required: Boolean,
      description: Option[String],
      schema: Schema
  ): ResponseHeader =
    new ResponseHeader(required, description, schema)

}

final class Parameter private (
    val name: String,
    val in: In,
    val required: Boolean,
    val description: Option[String],
    val schema: Schema // not specified in openapi spec but swagger-editor breaks without it for path parameters
) extends Serializable {

  override def toString: String =
    s"Parameter($name, $in, $required, $description, $schema)"

  override def equals(other: Any): Boolean =
    other match {
      case that: Parameter =>
        name == that.name && in == that.in && required == that.required &&
          description == that.description && schema == that.schema
      case _ => false
    }

  override def hashCode(): Int =
    Hashing.hash(name, in, required, description, schema)

  private[this] def copy(
      name: String = name,
      in: In = in,
      required: Boolean = required,
      description: Option[String] = description,
      schema: Schema = schema
  ): Parameter =
    new Parameter(name, in, required, description, schema)

  def withName(name: String): Parameter =
    copy(name = name)

  def withIn(in: In): Parameter =
    copy(in = in)

  def withDescription(description: Option[String]): Parameter =
    copy(description = description)

  def withSchema(schema: Schema): Parameter =
    copy(schema = schema)

}

object Parameter {

  def apply(
      name: String,
      in: In,
      required: Boolean,
      description: Option[String],
      schema: Schema
  ): Parameter =
    new Parameter(name, in, required, description, schema)

}

sealed trait In

object In {
  case object Query extends In
  case object Path extends In
  case object Header extends In
  case object Cookie extends In

  // All the possible values.
  val values: Seq[In] = Query :: Path :: Header :: Cookie :: Nil
}

final class MediaType private (val schema: Option[Schema]) extends Serializable {

  override def toString: String =
    s"Mediatype($schema)"

  override def equals(other: Any): Boolean =
    other match {
      case that: MediaType => schema == that.schema
      case _               => false
    }

  override def hashCode(): Int =
    Hashing.hash(schema)

  def withSchema(schema: Option[Schema]): MediaType = new MediaType(schema)
}

object MediaType {

  def apply(schema: Option[Schema]): MediaType = new MediaType(schema)

}

sealed trait Schema {
  def description: Option[String]
  def example: Option[ujson.Value]
  def title: Option[String]

  /** @return The same schema with its description overridden by the given `description`,
    *         or stay unchanged if this one is empty.
    */
  def withDefinedDescription(description: Option[String]): Schema =
    this match {
      case s: Schema.Object =>
        s.withDescription(description.orElse(s.description))
      case s: Schema.Array =>
        s.withDescription(description.orElse(s.description))
      case s: Schema.Enum =>
        s.withDescription(description.orElse(s.description))
      case s: Schema.Primitive =>
        s.withDescription(description.orElse(s.description))
      case s: Schema.OneOf =>
        s.withDescription(description.orElse(s.description))
      case s: Schema.AllOf =>
        s.withDescription(description.orElse(s.description))
      case s: Schema.Reference =>
        s.withDescription(description.orElse(s.description))
    }
}

object Schema {

  final class Object private (
      val properties: List[Property],
      val additionalProperties: Option[Schema],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Object($properties, $additionalProperties, $description, $example, $title)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Object =>
          properties == that.properties && additionalProperties == that.additionalProperties &&
            description == that.description && example == that.example &&
            title == that.title
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(
        properties,
        additionalProperties,
        description,
        example,
        title
      )

    private[this] def copy(
        properties: List[Property] = properties,
        additionalProperties: Option[Schema] = additionalProperties,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title
    ): Object =
      new Object(properties, additionalProperties, description, example, title)

    def withProperty(properties: List[Property]): Object =
      copy(properties = properties)

    def withAdditionalProperties(additionalProperties: Option[Schema]): Object =
      copy(additionalProperties = additionalProperties)

    def withDescription(description: Option[String]): Object =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): Object =
      copy(example = example)

    def withTitle(title: Option[String]): Object =
      copy(title = title)
  }

  object Object {

    def apply(
        properties: List[Property],
        additionalProperties: Option[Schema],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): Object =
      new Object(properties, additionalProperties, description, example, title)

  }

  final class Array private (
      val elementType: Either[Schema, List[Schema]],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Array($elementType, $description, $example, $title)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Array =>
          elementType == that.elementType && description == that.description && example == that.example && title == that.title
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(elementType, description, example, title)

    private[this] def copy(
        elementType: Either[Schema, List[Schema]] = elementType,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title
    ): Array =
      new Array(elementType, description, example, title)

    def withElementType(elementType: Either[Schema, List[Schema]]): Array =
      copy(elementType = elementType)

    def withDescription(description: Option[String]): Array =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): Array =
      copy(example = example)

    def withTitle(title: Option[String]): Array =
      copy(title = title)

  }

  object Array {

    def apply(
        elementType: Either[Schema, List[Schema]],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): Array =
      new Array(elementType, description, example, title)

  }

  final class Enum private (
      val elementType: Schema,
      val values: List[ujson.Value],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Enum($elementType, $values, $description, $example, $title)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Enum =>
          elementType == that.elementType && values == that.values && description == that.description &&
            example == that.example && title == that.title
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(elementType, values, description, example, title)

    private[this] def copy(
        elementType: Schema = elementType,
        values: List[ujson.Value] = values,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title
    ): Enum =
      new Enum(elementType, values, description, example, title)

    def withElementType(elementType: Schema): Enum =
      copy(elementType = elementType)

    def withValues(values: List[ujson.Value]): Enum =
      copy(values = values)

    def withDescription(description: Option[String]): Enum =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): Enum =
      copy(example = example)

    def withTitle(title: Option[String]): Enum =
      copy(title = title)
  }

  object Enum {

    def apply(
        elementType: Schema,
        values: List[ujson.Value],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): Enum = new Enum(elementType, values, description, example, title)

  }

  final class Property private (
      val name: String,
      val schema: Schema,
      val isRequired: Boolean,
      val description: Option[String]
  ) extends Serializable {

    override def toString: String =
      s"Property($name, $schema, $isRequired, $description)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Property =>
          name == that.name && schema == that.schema && isRequired == that.isRequired &&
            description == that.description
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(name, schema, isRequired, description)

    private[this] def copy(
        name: String = name,
        schema: Schema = schema,
        isRequired: Boolean = isRequired,
        description: Option[String] = description
    ): Property =
      new Property(name, schema, isRequired, description)

    def withName(name: String): Property =
      copy(name = name)

    def withSchema(schema: Schema): Property =
      copy(schema = schema)

    def withIsRequired(isRequired: Boolean): Property =
      copy(isRequired = isRequired)

    def withDescription(description: Option[String]): Property =
      copy(description = description)
  }

  object Property {

    def apply(
        name: String,
        schema: Schema,
        isRequired: Boolean,
        description: Option[String]
    ): Property = new Property(name, schema, isRequired, description)

  }

  final class Primitive private (
      val name: String,
      val format: Option[String],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String],
      val minimum: Option[Double] = None,
      val exclusiveMinimum: Option[Boolean] = None,
      val maximum: Option[Double] = None,
      val exclusiveMaximum: Option[Boolean] = None,
      val multipleOf: Option[Double] = None
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Primitive($name, $format, $description, $example, $title, $minimum, $exclusiveMinimum, $maximum, $exclusiveMaximum, $multipleOf)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Primitive =>
          name == that.name && format == that.format && description == that.description &&
            example == that.example && title == that.title && minimum == that.minimum &&
            exclusiveMinimum == that.exclusiveMinimum && maximum == that.maximum &&
            exclusiveMaximum == that.exclusiveMaximum && multipleOf == that.multipleOf
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(
        name,
        format,
        description,
        example,
        title,
        minimum,
        exclusiveMinimum,
        maximum,
        exclusiveMaximum,
        multipleOf
      )

    private[this] def copy(
        name: String = name,
        format: Option[String] = format,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title,
        minimum: Option[Double] = None,
        exclusiveMinimum: Option[Boolean] = None,
        maximum: Option[Double] = None,
        exclusiveMaximum: Option[Boolean] = None,
        multipleOf: Option[Double] = None
    ): Primitive =
      new Primitive(
        name,
        format,
        description,
        example,
        title,
        minimum,
        exclusiveMinimum,
        maximum,
        exclusiveMaximum,
        multipleOf
      )

    def withName(name: String): Primitive =
      copy(name = name)

    def withFormat(format: Option[String]): Primitive =
      copy(format = format)

    def withDescription(description: Option[String]): Primitive =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): Primitive =
      copy(example = example)

    def withTitle(title: Option[String]): Primitive =
      copy(title = title)

    def withMinimum(minimum: Option[Double]): Primitive =
      copy(minimum = minimum)

    def withExclusiveMinimum(exclusiveMinimum: Option[Boolean]): Primitive =
      copy(exclusiveMinimum = exclusiveMinimum)

    def withMaximum(maximum: Option[Double]): Primitive =
      copy(maximum = maximum)

    def withExclusiveMaximum(exclusiveMaximum: Option[Boolean]): Primitive =
      copy(exclusiveMaximum = exclusiveMaximum)

    def withMultipleOf(multipleOf: Option[Double]): Primitive =
      copy(multipleOf = multipleOf)

  }

  object Primitive {

    def apply(
        name: String,
        format: Option[String],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): Primitive =
      new Primitive(name, format, description, example, title)

  }

  final class OneOf private (
      val alternatives: Alternatives,
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"OneOf($alternatives, $description, $example, $title)"

    override def equals(other: Any): Boolean =
      other match {
        case that: OneOf =>
          alternatives == that.alternatives && description == that.description &&
            example == that.example && title == that.title
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(alternatives, description, example, title)

    private[this] def copy(
        alternatives: Alternatives = alternatives,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title
    ): OneOf =
      new OneOf(alternatives, description, example, title)

    def withAlternatives(alternatives: Alternatives): OneOf =
      copy(alternatives = alternatives)

    def withDescription(description: Option[String]): OneOf =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): OneOf =
      copy(example = example)

    def withTitle(title: Option[String]): OneOf =
      copy(title = title)

  }

  object OneOf {

    def apply(
        alternatives: Alternatives,
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): OneOf = new OneOf(alternatives, description, example, title)

  }

  sealed trait Alternatives

  final class DiscriminatedAlternatives private (
      val discriminatorFieldName: String,
      val alternatives: List[(String, Schema)]
  ) extends Alternatives
      with Serializable {

    override def toString: String =
      s"DiscriminatedAlternatives($discriminatorFieldName, $alternatives)"

    override def equals(other: Any): Boolean =
      other match {
        case that: DiscriminatedAlternatives =>
          discriminatorFieldName == that.discriminatorFieldName && alternatives == that.alternatives
        case _ =>
          false
      }

    override def hashCode(): Int =
      Hashing.hash(discriminatorFieldName, alternatives)

    private[this] def copy(
        discriminatorFieldName: String = discriminatorFieldName,
        alternatives: List[(String, Schema)] = alternatives
    ): DiscriminatedAlternatives =
      new DiscriminatedAlternatives(discriminatorFieldName, alternatives)

    def withDiscriminatorFieldName(
        discriminiatorFieldName: String
    ): DiscriminatedAlternatives =
      copy(discriminatorFieldName = discriminiatorFieldName)

    def withAlternatives(
        alternatives: List[(String, Schema)]
    ): DiscriminatedAlternatives =
      copy(alternatives = alternatives)
  }

  object DiscriminatedAlternatives {

    def apply(
        discriminatorFieldName: String,
        alternatives: List[(String, Schema)]
    ): DiscriminatedAlternatives =
      new DiscriminatedAlternatives(discriminatorFieldName, alternatives)

  }

  final class EnumeratedAlternatives private (
      val alternatives: List[Schema]
  ) extends Alternatives
      with Serializable {

    override def toString: String =
      s"EnumeratedAlternatives($alternatives)"

    override def equals(other: Any): Boolean =
      other match {
        case that: EnumeratedAlternatives => alternatives == that.alternatives
        case _                            => false
      }

    override def hashCode(): Int =
      Hashing.hash(alternatives)

    def withAlternatives(alternatives: List[Schema]): EnumeratedAlternatives =
      new EnumeratedAlternatives(alternatives)
  }

  object EnumeratedAlternatives {

    def apply(alternatives: List[Schema]): EnumeratedAlternatives =
      new EnumeratedAlternatives(alternatives)

  }

  final class AllOf private (
      val schemas: List[Schema],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"AllOf($schemas, $description, $example, $title)"

    override def equals(other: Any): Boolean =
      other match {
        case that: AllOf =>
          schemas == that.schemas && description == that.description &&
            example == that.example && title == that.title
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(schemas, description, example, title)

    private[this] def copy(
        schemas: List[Schema] = schemas,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title
    ): AllOf =
      new AllOf(schemas, description, example, title)

    def withSchemas(schemas: List[Schema]): AllOf =
      copy(schemas = schemas)

    def withDescription(description: Option[String]): AllOf =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): AllOf =
      copy(example = example)

    def withTitle(title: Option[String]): AllOf =
      copy(title = title)

  }

  object AllOf {

    def apply(
        schemas: List[Schema],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): AllOf = new AllOf(schemas, description, example, title)

  }

  final class Reference private (
      val name: String,
      val original: Option[Schema],
      val description: Option[String]
  ) extends Schema
      with Serializable {

    override val example: None.type = None // Reference objects can’t have examples
    override val title: None.type = None // Reference objects can’t have a title

    override def toString: String =
      s"Reference($name, $original, $description)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Reference =>
          name == that.name && original == that.original && description == that.description
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(name, original, description, example, title)

    private[this] def copy(
        name: String = name,
        original: Option[Schema] = original,
        description: Option[String] = description
    ) = new Reference(name, original, description)

    def withName(name: String): Reference = copy(name = name)

    def withOriginal(original: Option[Schema]): Reference =
      copy(original = original)

    def withDescription(description: Option[String]): Reference =
      copy(description = description)

  }

  object Reference {

    def apply(
        name: String,
        original: Option[Schema],
        description: Option[String]
    ): Reference = new Reference(name, original, description)

    def toRefPath(name: String): String =
      s"#/components/schemas/$name"
  }

  val simpleUUID = Primitive("string", format = Some("uuid"), None, None, None)
  val simpleString = Primitive("string", None, None, None, None)
  val simpleInteger = Primitive("integer", None, None, None, None)
  val simpleBoolean = Primitive("boolean", None, None, None, None)
  val simpleNumber = Primitive("number", None, None, None, None)

}

final class SecurityScheme private (
    val `type`: String, // TODO This should be a sealed trait, the `type` field should only exist in the JSON representation
    val description: Option[String],
    val name: Option[String],
    val in: Option[String], // TODO Create a typed enumeration
    val scheme: Option[String],
    val bearerFormat: Option[String]
) extends Serializable {

  override def toString: String =
    s"SecurityScheme(${`type`}, $description, $name, $in, $scheme, $bearerFormat)"

  override def equals(other: Any): Boolean =
    other match {
      case that: SecurityScheme =>
        `type` == that.`type` && description == that.description && name == that.name &&
          in == that.in && scheme == that.scheme && bearerFormat == that.bearerFormat
      case _ =>
        false
    }

  override def hashCode(): Int =
    Hashing.hash(`type`, description, name, in, scheme, bearerFormat)

  private[this] def copy(
      `type`: String = `type`,
      description: Option[String] = description,
      name: Option[String] = name,
      in: Option[String] = in,
      scheme: Option[String] = scheme,
      bearerFormat: Option[String] = bearerFormat
  ) = new SecurityScheme(`type`, description, name, in, scheme, bearerFormat)

  def withType(tpe: String): SecurityScheme =
    copy(`type` = tpe)

  def withDescription(description: Option[String]): SecurityScheme =
    copy(description = description)

  def withName(name: Option[String]): SecurityScheme =
    copy(name = name)

  def withIn(in: Option[String]): SecurityScheme =
    copy(in = in)

  def withScheme(scheme: Option[String]): SecurityScheme =
    copy(scheme = scheme)

  def withBearerFormat(bearerFormat: Option[String]): SecurityScheme =
    copy(bearerFormat = bearerFormat)

}

object SecurityScheme {

  def apply(
      `type`: String,
      description: Option[String],
      name: Option[String],
      in: Option[String],
      scheme: Option[String],
      bearerFormat: Option[String]
  ): SecurityScheme =
    new SecurityScheme(`type`, description, name, in, scheme, bearerFormat)

  def httpBasic: SecurityScheme =
    SecurityScheme(
      `type` = "http",
      description = Some("Http Basic Authentication"),
      name = None,
      in = None,
      scheme = Some("basic"),
      bearerFormat = None
    )
}
