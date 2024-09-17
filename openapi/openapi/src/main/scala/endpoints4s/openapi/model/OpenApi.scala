package endpoints4s.openapi.model

import endpoints4s.algebra.{ExternalDocumentationObject, Tag}
import endpoints4s.{Encoder, Hashing}

/** @see [[https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md]]
  * @note Throws an exception on creation if several tags have the same name but not the same other attributes.
  */
final class OpenApi private (
    val info: Info,
    private val innerPaths: collection.Map[String, PathItem],
    val components: Components,
    val servers: Seq[Server]
) extends Serializable {

  def paths: Map[String, PathItem] = innerPaths.toMap

  override def toString =
    s"OpenApi($info, $paths, $components, $servers)"

  override def equals(other: Any): Boolean =
    other match {
      case that: OpenApi =>
        info == that.info && paths == that.paths && components == that.components && servers == that.servers
      case _ => false
    }

  override def hashCode(): Int = Hashing.hash(info, paths, components, servers)

  val tags: Set[Tag] = OpenApi.extractTags(innerPaths)

  private[this] def copy(
      info: Info = info,
      paths: collection.Map[String, PathItem] = innerPaths,
      components: Components = components,
      servers: Seq[Server] = servers
  ): OpenApi =
    new OpenApi(info, paths, components, servers)

  def withInfo(info: Info): OpenApi =
    copy(info = info)

  def withPaths(paths: Map[String, PathItem]): OpenApi =
    copy(paths = paths)

  def withComponents(components: Components): OpenApi =
    copy(components = components)

  def withServers(servers: Seq[Server]): OpenApi =
    copy(servers = servers)
}

object OpenApi {

  val openApiVersion = "3.0.0"

  def apply(info: Info, paths: Map[String, PathItem], components: Components): OpenApi =
    new OpenApi(info, paths, components, Nil)

  def apply(info: Info, paths: collection.Map[String, PathItem], components: Components): OpenApi =
    new OpenApi(info, paths, components, Nil)

  def apply(
      info: Info,
      paths: collection.Map[String, PathItem],
      components: Components,
      servers: Seq[Server]
  ): OpenApi =
    new OpenApi(info, paths, components, servers)

  private def mapJson[A](map: collection.Map[String, A])(f: A => ujson.Value): ujson.Obj = {
    val result = ujson.Obj()
    //preserve order defined by user or sort by key to minimize diff
    val stableMap = map match {
      case map: collection.mutable.LinkedHashMap[String, A] => map
      case map: collection.immutable.ListMap[String, A]     => map
      case map                                              => map.toSeq.sortBy(_._1)
    }
    stableMap.foreach { case (k, v) => result.value.put(k, f(v)) }
    result
  }

  private[openapi] def schemaJson(schema: Schema): ujson.Obj = {
    val result = ujson.Obj()

    for (description <- schema.description) {
      result.value.put("description", ujson.Str(description))
    }
    for (example <- schema.example) {
      result.value.put("example", example)
    }
    for (title <- schema.title) {
      result.value.put("title", title)
    }
    for (default <- schema.default) {
      result.value.put("default", default)
    }

    schema match {
      case primitive: Schema.Primitive =>
        result.value.put("type", ujson.Str(primitive.name))
        primitive.format.foreach(s => result.value.put("format", ujson.Str(s)))
        primitive.minimum.foreach(d => result.value.put("minimum", ujson.Num(d)))
        primitive.exclusiveMinimum.foreach(b => result.value.put("exclusiveMinimum", ujson.Bool(b)))
        primitive.maximum.foreach(d => result.value.put("maximum", ujson.Num(d)))
        primitive.exclusiveMaximum.foreach(b => result.value.put("exclusiveMaximum", ujson.Bool(b)))
        primitive.multipleOf.foreach(d => result.value.put("multipleOf", ujson.Num(d)))
      case obj: Schema.Object =>
        result.value.put("type", "object")
        val properties = ujson.Obj()
        obj.properties.foreach { (p: Schema.Property) =>
          val schema = p.schema
            .withDefinedDescription(p.description)
            .withDefinedDefault(p.defaultValue)
          properties.value.put(p.name, schemaJson(schema))
        }
        result.value.put("properties", properties)

        val required = obj.properties.filter(_.isRequired).map(_.name)
        if (required.nonEmpty) {
          result.value.put("required", ujson.Arr.from(required))
        }
        obj.additionalProperties.foreach(p =>
          result.value.put("additionalProperties", schemaJson(p))
        )
      case array: Schema.Array =>
        result.value.put("type", "array")
        array.elementType match {
          case Left(value) =>
            result.value.put("items", schemaJson(value))
          case Right(value) =>
            // Best effort (not 100% accurate) to represent the heterogeneous array in OpenAPI 3.0
            // This should be changed with OpenAPI 3.1 and more idiomatic representation using `prefixItems`
            result.value ++= List(
              "items" -> schemaJson(
                Schema.OneOf(
                  alternatives = Schema.EnumeratedAlternatives(value),
                  description = None,
                  example = None,
                  title = None
                )
              ),
              "minItems" -> ujson.Num(value.length.toDouble),
              "maxItems" -> ujson.Num(value.length.toDouble)
            )
        }
      case enm: Schema.Enum =>
        result.value ++= schemaJson(
          enm.elementType.withDefinedDescription(enm.description)
        ).value
        result.value.put("enum", ujson.Arr.from(enm.values))
      case oneOf: Schema.OneOf =>
        result.value ++=
          (oneOf.alternatives match {
            case discAlternatives: Schema.DiscriminatedAlternatives =>
              val mapping = ujson.Obj()
              discAlternatives.alternatives.foreach {
                case (tag, ref: Schema.Reference) =>
                  mapping.value.put(tag, ujson.Str(Schema.Reference.toRefPath(ref.name)))
                case _ =>
              }
              val discriminator = ujson.Obj()
              discriminator.value += "propertyName" -> ujson.Str(
                discAlternatives.discriminatorFieldName
              )
              if (mapping.value.nonEmpty) {
                discriminator.value += "mapping" -> mapping
              }
              List(
                "oneOf" ->
                  ujson.Arr.from(discAlternatives.alternatives.map(kv => schemaJson(kv._2))),
                "discriminator" -> discriminator
              )
            case enumAlternatives: Schema.EnumeratedAlternatives =>
              List(
                "oneOf" -> ujson.Arr.from(enumAlternatives.alternatives.map(schemaJson))
              )
          })
      case allOf: Schema.AllOf =>
        result.value.put("allOf", ujson.Arr.from(allOf.schemas.map(schemaJson)))
      case reference: Schema.Reference =>
        /* In OpenAPI 3.0 (and 2.0), reference schemas are special in that all
         * their sibling values are ignored!
         *
         * This means that if any other sibling schema fields have been set
         * (eg. for a `description`, `example`, etc.), we need to nest the
         * schema reference object inside an `allOf` field.
         *
         * See <https://stackoverflow.com/a/41752575/3072788>.
         */
        val refSchemaName = ujson.Str(Schema.Reference.toRefPath(reference.name))
        if (result.value.isEmpty) {
          result.value.put("$ref", refSchemaName)
        } else {
          result.value.put("allOf", ujson.Arr(ujson.Obj("$ref" -> refSchemaName)))
        }
    }

    result
  }

  private def securitySchemeJson(securityScheme: SecurityScheme): ujson.Obj = {
    val result = ujson.Obj()
    result.value.put("type", ujson.Str(securityScheme.`type`))
    for (description <- securityScheme.description) {
      result.value.put("description", ujson.Str(description))
    }
    for (name <- securityScheme.name) {
      result.value.put("name", ujson.Str(name))
    }
    for (in <- securityScheme.in) {
      result.value.put("in", ujson.Str(in))
    }
    for (scheme <- securityScheme.scheme) {
      result.value.put("scheme", ujson.Str(scheme))
    }
    for (bearerFormat <- securityScheme.bearerFormat) {
      result.value.put("bearerFormat", ujson.Str(bearerFormat))
    }
    result
  }

  private def infoJson(info: Info): ujson.Obj = {
    val result = ujson.Obj()
    result.value.put("title", ujson.Str(info.title))
    result.value.put("version", ujson.Str(info.version))
    info.description.foreach(description => result.value.put("description", ujson.Str(description)))
    result
  }

  private def componentsJson(components: Components): ujson.Obj =
    ujson.Obj(
      "schemas" -> mapJson(components.schemas)(schemaJson),
      "securitySchemes" -> mapJson(components.securitySchemes)(
        securitySchemeJson
      )
    )

  private def responseJson(response: Response): ujson.Obj = {
    val result = ujson.Obj()
    result.value.put("description", ujson.Str(response.description))
    if (response.headers.nonEmpty) {
      result.value.put("headers", mapJson(response.headers)(responseHeaderJson))
    }
    if (response.content.nonEmpty) {
      result.value.put("content", mapJson(response.content)(mediaTypeJson))
    }
    result
  }

  def responseHeaderJson(responseHeader: ResponseHeader): ujson.Value = {
    val result = ujson.Obj()
    result.value.put("schema", schemaJson(responseHeader.schema))
    if (responseHeader.required) {
      result.value.put("required", ujson.True)
    }
    responseHeader.description.foreach { description =>
      result.value.put("description", ujson.Str(description))
    }
    result
  }

  def mediaTypeJson(mediaType: MediaType): ujson.Value =
    mediaType.schema match {
      case Some(schema) => ujson.Obj("schema" -> schemaJson(schema))
      case None         => ujson.Obj()
    }

  private def operationJson(operation: Operation): ujson.Obj = {
    val obj = ujson.Obj()
    obj.value.put("responses", mapJson(operation.responses)(responseJson))
    operation.operationId.foreach { id =>
      obj.value.put("operationId", ujson.Str(id))
    }
    operation.summary.foreach { summary =>
      obj.value.put("summary", ujson.Str(summary))
    }
    operation.description.foreach { description =>
      obj.value.put("description", ujson.Str(description))
    }
    if (operation.parameters.nonEmpty) {
      obj.value.put(
        "parameters",
        ujson.Arr.from(
          operation.parameters.map(parameterJson)
        )
      )
    }
    operation.requestBody.foreach { requestBody =>
      obj.value.put("requestBody", requestBodyJson(requestBody))
    }
    if (operation.tags.nonEmpty) {
      val tags = ujson.Arr()
      operation.tags.foreach(tag => tags.value += ujson.Str(tag.name))
      obj.value.put("tags", tags)
    }
    if (operation.security.nonEmpty) {
      val security = ujson.Arr()
      operation.security.foreach(item => security.value += securityRequirementJson(item))
      obj.value.put("security", security)
    }
    if (operation.callbacks.nonEmpty) {
      obj.value.put("callbacks", mapJson(operation.callbacks)(pathsJson))
    }
    if (operation.deprecated) {
      obj.value.put("deprecated", ujson.True)
    }
    obj
  }

  private def parameterJson(parameter: Parameter): ujson.Value = {
    val result = ujson.Obj(
      "name" -> ujson.Str(parameter.name),
      "in" -> inJson(parameter.in),
      "schema" -> schemaJson(parameter.schema)
    )
    parameter.description.foreach { description =>
      result.value.put("description", ujson.Str(description))
    }
    if (parameter.required) {
      result.value.put("required", ujson.True)
    }
    result
  }

  private def inJson(in: In): ujson.Value =
    in match {
      case In.Query  => ujson.Str("query")
      case In.Path   => ujson.Str("path")
      case In.Header => ujson.Str("header")
      case In.Cookie => ujson.Str("cookie")
    }

  private def requestBodyJson(body: RequestBody): ujson.Value = {
    val result = ujson.Obj()
    result.value.put("required", ujson.True)
    result.value.put("content", mapJson(body.content)(mediaTypeJson))
    body.description.foreach { description =>
      result.value.put("description", ujson.Str(description))
    }
    result
  }

  private def tagJson(tag: Tag): ujson.Value = {
    val result = ujson.Obj()
    result.value.put("name", ujson.Str(tag.name))

    for (description <- tag.description) {
      result.value.put("description", description)
    }
    for (externalDocs <- tag.externalDocs) {
      result.value.put("externalDocs", externalDocumentationObjectJson(externalDocs))
    }
    result
  }

  private def serverJson(server: Server): ujson.Value = {
    val result = ujson.Obj()
    result.value.put("url", ujson.Str(server.url))
    for (description <- server.description) {
      result.value.put("description", ujson.Str(description))
    }
    if (server.variables.nonEmpty) {
      result.value.put("variables", mapJson(server.variables)(serverVariableJson))
    }
    result
  }

  private def serverVariableJson(variable: ServerVariable): ujson.Value = {
    val result = ujson.Obj()
    result.value.put("default", ujson.Str(variable.default))
    for (description <- variable.description) {
      result.value.put("description", ujson.Str(description))
    }
    for (alternatives <- variable.`enum`) {
      result.value.put(
        "enum",
        ujson.Arr.from(alternatives.map(alternative => ujson.Str(alternative)))
      )
    }
    result
  }

  private def externalDocumentationObjectJson(
      externalDoc: ExternalDocumentationObject
  ): ujson.Value = {
    val result = ujson.Obj(
      "url" -> ujson.Str(externalDoc.url)
    )
    for (description <- externalDoc.description) {
      result.value.put("description", description)
    }
    result
  }

  private def securityRequirementJson(
      securityRequirement: SecurityRequirement
  ): ujson.Value =
    ujson.Obj(
      securityRequirement.name -> ujson.Arr.from(securityRequirement.scopes.map(ujson.Str))
    )

  private def pathsJson(paths: collection.Map[String, PathItem]): ujson.Obj =
    mapJson(paths)(pathItem => mapJson(pathItem.operations)(operationJson))

  val jsonEncoder: Encoder[OpenApi, ujson.Value] =
    openApi => {
      val result = ujson.Obj()
      result.value.put("openapi", ujson.Str(openApiVersion))
      result.value.put("info", infoJson(openApi.info))
      result.value.put("paths", pathsJson(openApi.innerPaths))

      if (openApi.servers.nonEmpty) {
        val servers = ujson.Arr()
        openApi.servers.foreach(server => servers.value += serverJson(server))
        result.value.put("servers", servers)
      }
      if (openApi.tags.nonEmpty) {
        val tagsAsJson = openApi.tags.map(tag => tagJson(tag)).toList
        result.value.put("tags", ujson.Arr.from(tagsAsJson))
      }
      if (openApi.components.schemas.nonEmpty || openApi.components.securitySchemes.nonEmpty) {
        result.value.put("components", componentsJson(openApi.components))
      }
      result.value
    }

  private def extractTags(paths: collection.Map[String, PathItem]): Set[Tag] = {
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

/** @see [[https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#server-object]] */
final class Server private (
    val url: String,
    val description: Option[String],
    val variables: Map[String, ServerVariable]
) extends Serializable {

  override def toString: String = s"Server($url, $description, $variables)"

  override def equals(other: Any): Boolean =
    other match {
      case that: Server =>
        url == that.url && description == that.description && variables == that.variables
      case _ => false
    }

  override def hashCode(): Int = Hashing.hash(url, description, variables)

  private[this] def copy(
      url: String = url,
      description: Option[String] = description,
      variables: Map[String, ServerVariable] = variables
  ): Server =
    new Server(url, description, variables)

  def withUrl(url: String): Server = copy(url = url)

  def withDescription(description: Option[String]): Server = copy(description = description)

  def withVariables(variables: Map[String, ServerVariable]): Server = copy(variables = variables)
}

object Server {

  def apply(url: String): Server =
    new Server(url, None, Map.empty)

}

/** @see [[https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.3.md#server-variable-object]] */
final class ServerVariable private (
    val default: String,
    val `enum`: Option[Seq[String]],
    val description: Option[String]
) extends Serializable {
  assert(
    `enum`.forall(_.nonEmpty),
    "The enumeration of the values to be used in the substitution must not be empty."
  )

  override def toString = s"ServerVariable($default, ${`enum`}, $description)"

  override def equals(other: Any): Boolean = other match {
    case that: ServerVariable =>
      default == that.default &&
        `enum` == that.`enum` &&
        description == that.description
    case _ => false
  }

  override def hashCode(): Int = Hashing.hash(default, `enum`, description)

  private[this] def copy(
      default: String = default,
      `enum`: Option[Seq[String]] = `enum`,
      description: Option[String] = description
  ): ServerVariable =
    new ServerVariable(default, `enum`, description)

  def withDefault(default: String): ServerVariable = copy(default = default)

  def withEnum(`enum`: Option[Seq[String]]): ServerVariable = copy(`enum` = `enum`)

  def withDescription(description: Option[String]): ServerVariable = copy(description = description)

}

object ServerVariable {
  def apply(default: String): ServerVariable =
    new ServerVariable(default, None, None)

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
  def default: Option[ujson.Value]
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

  /** @return The same schema with its default overridden by the given `default`,
    *         or stay unchanged if this one is empty.
    */
  def withDefinedDefault(default: Option[ujson.Value]): Schema =
    this match {
      case s: Schema.Object =>
        s.withDefault(default.orElse(s.default))
      case s: Schema.Array =>
        s.withDefault(default.orElse(s.default))
      case s: Schema.Enum =>
        s.withDefault(default.orElse(s.default))
      case s: Schema.Primitive =>
        s.withDefault(default.orElse(s.default))
      case s: Schema.OneOf =>
        s.withDefault(default.orElse(s.default))
      case s: Schema.AllOf =>
        s.withDefault(default.orElse(s.default))
      case s: Schema.Reference =>
        s.withDefault(default.orElse(s.default))
    }
}

object Schema {

  final class Object private (
      val properties: List[Property],
      val additionalProperties: Option[Schema],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String],
      val default: Option[ujson.Value]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Object($properties, $additionalProperties, $description, $example, $title, $default)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Object =>
          properties == that.properties && additionalProperties == that.additionalProperties &&
            description == that.description && example == that.example &&
            title == that.title && default == that.default
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(
        properties,
        additionalProperties,
        description,
        example,
        title,
        default
      )

    private[this] def copy(
        properties: List[Property] = properties,
        additionalProperties: Option[Schema] = additionalProperties,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title,
        default: Option[ujson.Value] = default
    ): Object =
      new Object(properties, additionalProperties, description, example, title, default)

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

    def withDefault(default: Option[ujson.Value]): Object =
      copy(default = default)
  }

  object Object {

    def apply(
        properties: List[Property],
        additionalProperties: Option[Schema],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): Object =
      new Object(properties, additionalProperties, description, example, title, None)

  }

  final class Array private (
      val elementType: Either[Schema, List[Schema]],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String],
      val default: Option[ujson.Value]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Array($elementType, $description, $example, $title, $default)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Array =>
          elementType == that.elementType && description == that.description &&
            example == that.example && title == that.title && default == that.default
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(elementType, description, example, title, default)

    private[this] def copy(
        elementType: Either[Schema, List[Schema]] = elementType,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title,
        default: Option[ujson.Value] = default
    ): Array =
      new Array(elementType, description, example, title, default)

    def withElementType(elementType: Either[Schema, List[Schema]]): Array =
      copy(elementType = elementType)

    def withDescription(description: Option[String]): Array =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): Array =
      copy(example = example)

    def withTitle(title: Option[String]): Array =
      copy(title = title)

    def withDefault(default: Option[ujson.Value]): Array =
      copy(default = default)

  }

  object Array {

    def apply(
        elementType: Either[Schema, List[Schema]],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): Array =
      new Array(elementType, description, example, title, None)

  }

  final class Enum private (
      val elementType: Schema,
      val values: List[ujson.Value],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String],
      val default: Option[ujson.Value]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Enum($elementType, $values, $description, $example, $title, $default)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Enum =>
          elementType == that.elementType && values == that.values && description == that.description &&
            example == that.example && title == that.title && default == that.default
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(elementType, values, description, example, title, default)

    private[this] def copy(
        elementType: Schema = elementType,
        values: List[ujson.Value] = values,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title,
        default: Option[ujson.Value] = default
    ): Enum =
      new Enum(elementType, values, description, example, title, default)

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

    def withDefault(default: Option[ujson.Value]): Enum =
      copy(default = default)
  }

  object Enum {

    def apply(
        elementType: Schema,
        values: List[ujson.Value],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): Enum = new Enum(elementType, values, description, example, title, None)

  }

  final class Property private (
      val name: String,
      val schema: Schema,
      val isRequired: Boolean,
      val defaultValue: Option[ujson.Value],
      val description: Option[String]
  ) extends Serializable {

    def this(
        name: String,
        schema: Schema,
        isRequired: Boolean,
        description: Option[String]
    ) = this(name, schema, isRequired, None, description)

    override def toString: String =
      s"Property($name, $schema, $isRequired, $defaultValue, $description)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Property =>
          name == that.name && schema == that.schema && isRequired == that.isRequired &&
            defaultValue == that.defaultValue && description == that.description
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(name, schema, isRequired, defaultValue, description)

    private[this] def copy(
        name: String = name,
        schema: Schema = schema,
        isRequired: Boolean = isRequired,
        defaultValue: Option[ujson.Value] = defaultValue,
        description: Option[String] = description
    ): Property =
      new Property(name, schema, isRequired, defaultValue, description)

    def withName(name: String): Property =
      copy(name = name)

    def withSchema(schema: Schema): Property =
      copy(schema = schema)

    def withIsRequired(isRequired: Boolean): Property =
      copy(isRequired = isRequired)

    def withDefaultValue(defaultValue: Option[ujson.Value]): Property =
      copy(defaultValue = defaultValue)

    def withDescription(description: Option[String]): Property =
      copy(description = description)
  }

  object Property {

    def apply(
        name: String,
        schema: Schema,
        isRequired: Boolean,
        defaultValue: Option[ujson.Value],
        description: Option[String]
    ): Property = new Property(name, schema, isRequired, defaultValue, description)

    def apply(
        name: String,
        schema: Schema,
        isRequired: Boolean,
        description: Option[String]
    ): Property = new Property(name, schema, isRequired, None, description)
  }

  final class Primitive private (
      val name: String,
      val format: Option[String],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String],
      val default: Option[ujson.Value],
      val minimum: Option[Double],
      val exclusiveMinimum: Option[Boolean],
      val maximum: Option[Double],
      val exclusiveMaximum: Option[Boolean],
      val multipleOf: Option[Double]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Primitive($name, $format, $description, $example, $title, $default, $minimum, $exclusiveMinimum, $maximum, $exclusiveMaximum, $multipleOf)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Primitive =>
          name == that.name && format == that.format && description == that.description &&
            example == that.example && title == that.title && default == that.default &&
            minimum == that.minimum && exclusiveMinimum == that.exclusiveMinimum &&
            maximum == that.maximum && exclusiveMaximum == that.exclusiveMaximum &&
            multipleOf == that.multipleOf
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(
        name,
        format,
        description,
        example,
        title,
        default,
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
        default: Option[ujson.Value] = default,
        minimum: Option[Double] = minimum,
        exclusiveMinimum: Option[Boolean] = exclusiveMinimum,
        maximum: Option[Double] = maximum,
        exclusiveMaximum: Option[Boolean] = exclusiveMaximum,
        multipleOf: Option[Double] = multipleOf
    ): Primitive =
      new Primitive(
        name,
        format,
        description,
        example,
        title,
        default,
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

    def withDefault(default: Option[ujson.Value]): Primitive =
      copy(default = default)

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
      new Primitive(name, format, description, example, title, None, None, None, None, None, None)

  }

  final class OneOf private (
      val alternatives: Alternatives,
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String],
      val default: Option[ujson.Value]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"OneOf($alternatives, $description, $example, $title, $default)"

    override def equals(other: Any): Boolean =
      other match {
        case that: OneOf =>
          alternatives == that.alternatives && description == that.description &&
            example == that.example && title == that.title && default == that.default
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(alternatives, description, example, title, default)

    private[this] def copy(
        alternatives: Alternatives = alternatives,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title,
        default: Option[ujson.Value] = default
    ): OneOf =
      new OneOf(alternatives, description, example, title, default)

    def withAlternatives(alternatives: Alternatives): OneOf =
      copy(alternatives = alternatives)

    def withDescription(description: Option[String]): OneOf =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): OneOf =
      copy(example = example)

    def withTitle(title: Option[String]): OneOf =
      copy(title = title)

    def withDefault(default: Option[ujson.Value]): OneOf =
      copy(default = default)

  }

  object OneOf {

    def apply(
        alternatives: Alternatives,
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): OneOf = new OneOf(alternatives, description, example, title, None)

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
      val title: Option[String],
      val default: Option[ujson.Value]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"AllOf($schemas, $description, $example, $title, $default)"

    override def equals(other: Any): Boolean =
      other match {
        case that: AllOf =>
          schemas == that.schemas && description == that.description &&
            example == that.example && title == that.title && default == that.default
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(schemas, description, example, title, default)

    private[this] def copy(
        schemas: List[Schema] = schemas,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title,
        default: Option[ujson.Value] = default
    ): AllOf =
      new AllOf(schemas, description, example, title, default)

    def withSchemas(schemas: List[Schema]): AllOf =
      copy(schemas = schemas)

    def withDescription(description: Option[String]): AllOf =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): AllOf =
      copy(example = example)

    def withTitle(title: Option[String]): AllOf =
      copy(title = title)

    def withDefault(default: Option[ujson.Value]): AllOf =
      copy(default = default)

  }

  object AllOf {

    def apply(
        schemas: List[Schema],
        description: Option[String],
        example: Option[ujson.Value],
        title: Option[String]
    ): AllOf = new AllOf(schemas, description, example, title, None)

  }

  final class Reference private (
      val name: String,
      val original: Option[Schema],
      val description: Option[String],
      val example: Option[ujson.Value],
      val title: Option[String], // you probably want the title on the original schema!
      val default: Option[ujson.Value]
  ) extends Schema
      with Serializable {

    override def toString: String =
      s"Reference($name, $original, $description, $example, $title, $default)"

    override def equals(other: Any): Boolean =
      other match {
        case that: Reference =>
          name == that.name && original == that.original && description == that.description &&
            example == that.example && title == that.title && default == that.default
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(name, original, description, example, title, default)

    private[this] def copy(
        name: String = name,
        original: Option[Schema] = original,
        description: Option[String] = description,
        example: Option[ujson.Value] = example,
        title: Option[String] = title,
        default: Option[ujson.Value] = default
    ) = new Reference(name, original, description, example, title, default)

    def withName(name: String): Reference = copy(name = name)

    def withOriginal(original: Option[Schema]): Reference =
      copy(original = original)

    def withDescription(description: Option[String]): Reference =
      copy(description = description)

    def withExample(example: Option[ujson.Value]): Reference =
      copy(example = example)

    def withTitle(description: Option[String]): Reference =
      copy(description = description)

    def withDefault(default: Option[ujson.Value]): Reference =
      copy(default = default)
  }

  object Reference {

    def apply(
        name: String,
        original: Option[Schema],
        description: Option[String]
    ): Reference = new Reference(name, original, description, None, None, None)

    def toRefPath(name: String): String =
      s"#/components/schemas/$name"
  }

  val simpleUUID: Primitive = Primitive("string", format = Some("uuid"), None, None, None)
  val simpleString: Primitive = Primitive("string", None, None, None, None)
  val simpleInteger: Primitive = Primitive("integer", format = Some("int32"), None, None, None)
  val simpleLong: Primitive = Primitive("integer", format = Some("int64"), None, None, None)
  val simpleBoolean: Primitive = Primitive("boolean", None, None, None, None)
  val simpleNumber: Primitive = Primitive("number", format = Some("double"), None, None, None)

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
