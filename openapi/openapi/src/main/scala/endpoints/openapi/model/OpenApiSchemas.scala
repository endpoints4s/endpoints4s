package endpoints.openapi.model

import endpoints.algebra.JsonSchemas

/**
  * Provides an implicit instance of `JsonSchema[OpenApi]` which
  * can be interpreted by a proper JSON encoder (e.g.,
  * `endpoints-json-schema-playjson`) to produce a JSON document
  * out of an `OpenApi` value.
  */
trait OpenApiSchemas extends JsonSchemas {

  val openapiVersion = "3.0.0"

  implicit lazy val openApiSchema: JsonSchema[OpenApi] = (
    field[String]("openapi") zip
    field[Info]("info") zip
    field[Map[String, PathItem]]("paths") zip
    optField[Components]("components")
  ).xmap[OpenApi]{
    // TODO Reject if openapi version does not match our openapiVersion
    case (((_, info), paths), components) => OpenApi(info, paths, components.getOrElse(Components(Map.empty, Map.empty)))
  } {
    o =>
      val components =
        if (o.components.schemas.isEmpty && o.components.securitySchemes.isEmpty) None
        else Some(o.components)
      (((openapiVersion, o.info), o.paths), components)
  }

  implicit lazy val infoSchema: JsonSchema[Info] = (
      field[String]("title") zip
      field[String]("version")
    ).xmap(Info.tupled)(info => (info.title, info.version))

  implicit lazy val pathItemSchema: JsonSchema[PathItem] =
    mapJsonSchema[Operation].xmap(PathItem(_))(_.operations)

  implicit lazy val operationSchema: JsonSchema[Operation] = (
    optField [String]                    ("summary")     zip
    optField [String]                    ("description") zip
    optField [List[Parameter]]           ("parameters")  zip
    optField [RequestBody]               ("requestBody") zip
       field [Map[String, Response]]     ("responses")   zip
    optField [List[String]]              ("tags")        zip
    optField [List[SecurityRequirement]] ("security")
  ).xmap[Operation] {
    case ((((((summary, description), parameters), requestBody), responses), tags), security) =>
      Operation(summary, description, parameters.getOrElse(Nil), requestBody, responses, tags.getOrElse(Nil), security.getOrElse(Nil))
  } {
    o => ((((((o.summary, o.description), if (o.parameters.isEmpty) None else Some(o.parameters)), o.requestBody), o.responses), if (o.tags.isEmpty) None else Some(o.tags)), if (o.security.isEmpty) None else Some(o.security))
  }

  implicit lazy val parameterSchema: JsonSchema[Parameter] = (
       field [String]  ("name")        zip
       field [In]      ("in")          zip
       field [Schema]  ("schema")      zip
    optField [String]  ("description") zip
    optField [Boolean] ("required")
  ).xmap[Parameter] {
    case ((((name, in), schema), description), required) => Parameter(name, in, required.contains(true), description, schema)
  } {
    p => ((((p.name, p.in), p.schema), p.description), if (p.required) Some(true) else None)
  }

  implicit lazy val inSchema: JsonSchema[In] =
    enumeration[In](In.values) {
      case In.Cookie => "cookie"
      case In.Header => "header"
      case In.Path   => "path"
      case In.Query  => "query"
    }

  implicit lazy val requestBodySchema: JsonSchema[RequestBody] = (
       field [Map[String, MediaType]] ("content")     zip
    optField [String]                 ("description")
  ).xmap[RequestBody] {
    case (content, description) => RequestBody(description, content)
  } {
    r => (r.content, r.description)
  }

  implicit lazy val mediaTypeSchema: JsonSchema[MediaType] = (
    optField [Schema] ("schema")
  ).xmap[MediaType](MediaType(_))(_.schema)

  implicit lazy val responseSchema: JsonSchema[Response] = (
       field [String]                 ("description") zip
    optField [Map[String, MediaType]] ("content")
  ).xmap[Response] {
    case (description, content) => Response(description, content.getOrElse(Map.empty))
  } {
    r => (r.description, if (r.content.isEmpty) None else Some(r.content))
  }

  implicit lazy val securityRequirementSchema: JsonSchema[SecurityRequirement] =
    mapJsonSchema[List[String]].xmap[SecurityRequirement] {
      s =>
        val (name, scopes) = s.head // TODO Better failure handling
        SecurityRequirement(name, ???, scopes) // TODO We’d need the `Components` to look up for the `SecurityScheme`
    } {
      s => Map(s.name -> s.scopes)
    }

  implicit lazy val componentsSchema: JsonSchema[Components] = (
    field [Map[String, Schema]]         ("schemas")         zip // FIXME Optional?
    field [Map[String, SecurityScheme]] ("securitySchemes") // FIXME Optional?
  ).xmap[Components] {
    case (schemas, securitySchemes) => Components(schemas, securitySchemes)
  } {
    components => (components.schemas, components.securitySchemes)
  }

  private def schemaSchemaRef: JsonSchema[Schema] = lazySchema(schemaSchema, "Schema")
  implicit lazy val schemaSchema: JsonSchema[Schema] = (
    optField [String]              ("type")          zip
    optField [String]              ("format")        zip
    optField [Schema]              ("items")(schemaSchemaRef) zip
    optField [Map[String, Schema]] ("properties")(mapJsonSchema(schemaSchemaRef)) zip
    optField [Schema]              ("additionalProperties")(schemaSchemaRef) zip
    optField [List[String]]        ("required")      zip
    optField [List[Schema]]        ("oneOf")(arrayJsonSchema[List, Schema](schemaSchemaRef, implicitly)) zip
    optField [DiscriminatorFields] ("discriminator") zip
    optField [List[Schema]]        ("allOf")(arrayJsonSchema[List, Schema](schemaSchemaRef, implicitly)) zip
    optField [List[String]]        ("enum")          zip
    optField [String]              ("$ref")          zip
    optField [String]              ("description")
  ).xmap[Schema]{
    case (((((((((((Some("integer"), format), _), _), _), _), _), _), _), _), _), description) => Schema.Primitive("integer", format, description)
    case (((((((((((Some("string"), format), _), _), _), _), _), _), _), _), _), description)  => Schema.Primitive("string", format, description)
    case (((((((((((Some("object"), _), _), Some(props)), additionalProperties), required), _), _), _), _), _), description) =>
      // HACK We don’t decode properties descriptions
      val properties =
        props.map { case (n, s) => Schema.Property(n, s, required.forall(_.contains(n)), None) }.toList
      Schema.Object(properties, additionalProperties, description)
    case _ => ??? // TODO Complete. This is not really important because we don’t claim that we support *decoding* OpenAPI documents
  } { schema =>
    val fields = schemaToFields(schema)
    (((((((((((fields.`type`, fields.format), fields.items), fields.properties), fields.additionalProperties), fields.required.map(_.toList)), fields.oneOf), fields.discriminator), fields.allOf), fields.enum), fields.ref), fields.description)
  }

  private def schemaToFields(schema: Schema): SchemaFields = schema match {
    case Schema.Primitive(name, format, description) => SchemaFields(`type` = Some(name), format, description = description)
    case Schema.Array(itemsSchema, description) => SchemaFields(`type` = Some("array"), items = Some(itemsSchema), description = description)
    case Schema.Object(props, additionalProperties, descr) =>
      val propsMap = props.map(p => p.name -> p.schema.withDefinedDescription(p.description)).toMap
      val required = props.filter(_.isRequired).map(_.name).toSet
      SchemaFields(
        `type` = Some("object"),
        properties = Some(propsMap),
        additionalProperties = additionalProperties,
        required = if (required.isEmpty) None else Some(required),
        description = descr
      )
    case Schema.OneOf(discriminatorName, alternatives, description) =>
      val mapping =
        alternatives.collect { case (tag, ref: Schema.Reference) => tag -> Schema.Reference.toRefPath(ref.name) }.toMap
      val discriminatorFields =
        DiscriminatorFields(
          propertyName = discriminatorName,
          mapping = mapping
        )
      SchemaFields(oneOf = Some(alternatives.map(_._2)), discriminator = Some(discriminatorFields), description = description)
    case Schema.AllOf(schemas, description) => SchemaFields(allOf = Some(schemas), description = description)
    case Schema.Enum(elementType, values, description) =>
      schemaToFields(elementType.withDefinedDescription(description)).copy(enum = Some(values))
    case Schema.Reference(name, _, description) => SchemaFields(ref = Some(Schema.Reference.toRefPath(name)), description = description)
  }

  // Schemas can be encoded in various forms. Instead of using a sealed
  // trait to enumerate all the alternatives, we use a single `SchemaFields`
  // case class with all fields optionals.
  case class SchemaFields(
    `type`: Option[String] = None,
    format: Option[String] = None,
    items: Option[Schema] = None,
    properties: Option[Map[String, Schema]] = None,
    additionalProperties: Option[Schema] = None,
    required: Option[Set[String]] = None,
    oneOf: Option[List[Schema]] = None,
    discriminator: Option[DiscriminatorFields] = None,
    allOf: Option[List[Schema]] = None,
    enum: Option[List[String]] = None,
    ref: Option[String] = None,
    description: Option[String] = None
  )
  case class DiscriminatorFields(propertyName: String, mapping: Map[String, String])

  implicit lazy val discriminatorSchema: JsonSchema[DiscriminatorFields] = (
    field [String]              ("propertyName") zip
    field [Map[String, String]] ("mapping")
  ).xmap[DiscriminatorFields] {
    case (propertyName, mapping) => DiscriminatorFields(propertyName, mapping)
  } {
    d => (d.propertyName, d.mapping)
  }

  implicit lazy val securitySchemeSchema: JsonSchema[SecurityScheme] = (
       field [String] ("type")         zip
    optField [String] ("description")  zip
    optField [String] ("name")         zip
    optField [String] ("in")           zip
    optField [String] ("scheme")       zip
    optField [String] ("bearerFormat")
  ).xmap[SecurityScheme] {
    case (((((tpe, description), name), in), scheme), bearerFormat) => SecurityScheme(tpe, description, name, in, scheme, bearerFormat)
  } {
    ss => (((((ss.`type`, ss.description), ss.name), ss.in), ss.scheme), ss.bearerFormat)
  }

}
