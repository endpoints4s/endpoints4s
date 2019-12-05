package endpoints
package openapi

import endpoints.openapi.model.{MediaType, Schema}

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that produces a documentation of the JSON schemas.
  *
  * @group interpreters
  */
trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with EndpointsWithCustomErrors
    with JsonSchemas {

  import DocumentedJsonSchema._

  def jsonRequest[A](implicit codec: JsonSchema[A]) = Map("application/json" -> MediaType(Some(toSchema(codec.docs))))

  def jsonResponse[A](implicit codec: JsonSchema[A]) = Map("application/json" -> MediaType(Some(toSchema(codec.docs))))

  def toSchema(jsonSchema: DocumentedJsonSchema): Schema =
    toSchema(jsonSchema, None, Set.empty)

  private def toSchema(documentedCodec: DocumentedJsonSchema, coprodBase: Option[(String, DocumentedCoProd)], referencedSchemas: Set[String]): Schema = {
    documentedCodec match {
      case record @ DocumentedRecord(_, _, Some(name)) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(expandRecordSchema(record, coprodBase, referencedSchemas + name)), None)
      case record @ DocumentedRecord(_, _, None) =>
        expandRecordSchema(record, coprodBase, referencedSchemas)
      case coprod @ DocumentedCoProd(_, Some(name), _) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(expandCoproductSchema(coprod, referencedSchemas + name)), None)
      case coprod @ DocumentedCoProd(_, None, _) =>
        expandCoproductSchema(coprod, referencedSchemas)
      case Primitive(name, format) =>
        Schema.Primitive(name, format, None)
      case Array(Left(elementType)) =>
        Schema.Array(Left(toSchema(elementType, coprodBase, referencedSchemas)), None)
      case Array(Right(elementTypes)) =>
        Schema.Array(Right(elementTypes.map(elementType => toSchema(elementType, coprodBase, referencedSchemas))), None)
      case DocumentedEnum(elementType, values, Some(name)) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas + name), values, None)), None)
      case DocumentedEnum(elementType, values, None) =>
        Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas), values, None)
      case lzy: LazySchema =>
        toSchema(lzy.value, coprodBase, referencedSchemas)
    }
  }

  private def expandRecordSchema(record: DocumentedJsonSchema.DocumentedRecord, coprodBase: Option[(String, DocumentedCoProd)], referencedSchemas: Set[String]): Schema = {
    val fieldsSchema = record.fields
      .map(f => Schema.Property(f.name, toSchema(f.tpe, None, referencedSchemas), !f.isOptional, f.documentation))

    val additionalProperties = record.additionalProperties.map(toSchema(_, None, referencedSchemas))

    coprodBase.fold[Schema] {
      Schema.Object(fieldsSchema, additionalProperties, None)
    } { case (tag, coprod) =>
      val discriminatorField =
        Schema.Property(
          coprod.discriminatorName,
          Schema.Enum(Schema.simpleString, List(tag), None),
          isRequired = true,
          description = None
        )

      coprod.name.fold[Schema] {
        Schema.Object(discriminatorField :: fieldsSchema, additionalProperties, None)
      } { coproductName =>
        Schema.AllOf(
          schemas = List(
            Schema.Reference(coproductName, None, None),
            Schema.Object(discriminatorField :: fieldsSchema, additionalProperties, None)
          ),
          description = None
        )
      }
    }
  }

  private def expandCoproductSchema(coprod: DocumentedJsonSchema.DocumentedCoProd, referencedSchemas: Set[String]): Schema = {
    val alternativesSchemas =
      coprod.alternatives.map { case (tag, record) => tag -> toSchema(record, Some(tag -> coprod), referencedSchemas) }
    Schema.OneOf(coprod.discriminatorName, alternativesSchemas, None)
  }
}
