package endpoints.openapi.model

import endpoints.algebra.JsonSchemas

trait OpenApiSchemas extends JsonSchemas {

  implicit lazy val openApiSchema: JsonSchema[OpenApi] = (
      field[String]("openapi") zip
      field[Info]("info") zip
      field[Map[String, PathItem]]("paths") zip
      optField[Components]("components")
    ).invmap[OpenApi](notImplemented)(o => ((("3.0.0", o.info), o.paths), if (o.components.schemas.isEmpty) None else Some(o.components))) // TODO pull openapi version into model?

  implicit lazy val infoSchema: JsonSchema[Info] = (
      field[String]("title") zip
      field[String]("version")
    ).invmap(Info.tupled)(info => (info.title, info.version))

  implicit lazy val pathItemSchema: JsonSchema[PathItem] =
    mapJsonSchema(operationSchema).invmap(notImplemented)(_.operations)

  implicit lazy val operationSchema: JsonSchema[Operation] = (
    optField[String]("summary") zip
    optField[String]("description")
  ).invmap[Operation](notImplemented)(o => (o.summary, o.description))

  implicit lazy val componentsSchema: JsonSchema[Components] =
    field[Map[String, Schema]]("schemas")
      .invmap[Components](notImplemented)(_.schemas)

  implicit lazy val schemaSchema: JsonSchema[Schema] = (
    optField[String]("type") zip
    optField[String]("format") zip
    optField[Schema]("items")
  ).invmap[Schema](notImplemented) { schema =>
    val fields = schemaToFields(schema)
    ((fields.`type`, fields.format), fields.items)
  }

  private def schemaToFields(schema: Schema): SchemaFields = schema match {
    case Schema.Primitive(name, None) => SchemaFields(`type` = Some(name))
    case Schema.Primitive(name, Some(format)) => SchemaFields(`type` = Some(name), format = Some(format))
    case Schema.Array(itemsSchema) => SchemaFields(`type` = Some("array"), items = Some(itemsSchema))
  }
  case class SchemaFields(
                           `type`: Option[String] = None,
                           format: Option[String] = None,
                           items: Option[Schema] = None
                         )

  private def notImplemented(a: Any): Nothing = ???

}
