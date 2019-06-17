package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model.{MediaType, Schema}

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that produces a documentation of the JSON schemas.
  *
  * @group interpreters
  */
trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with Endpoints
    with JsonSchemas {

  import DocumentedJsonSchema._

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonSchema[A]): Option[DocumentedRequestEntity] =
    Some(DocumentedRequestEntity(docs, Map("application/json" -> MediaType(Some(toSchema(codec))))))

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonSchema[A]): List[DocumentedResponse] =
    DocumentedResponse(OK, docs.getOrElse(""), Map("application/json" -> MediaType(Some(toSchema(codec))))) :: Nil

  def toSchema(jsonSchema: DocumentedJsonSchema): Schema =
    toSchema(jsonSchema, None, Set.empty)

  private def toSchema(documentedCodec: DocumentedJsonSchema, coprodBase: Option[DocumentedCoProd], referencedSchemas: Set[String]): Schema = {
    documentedCodec match {
      case record @ DocumentedRecord(_, _, Some(name)) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(expandRecordSchema(record, coprodBase, referencedSchemas + name)), None)
      case record @ DocumentedRecord(_, _, None) =>
        expandRecordSchema(record, None, referencedSchemas)
      case coprod @ DocumentedCoProd(_, Some(name), _) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(expandCoproductSchema(coprod, referencedSchemas + name)), None)
      case coprod @ DocumentedCoProd(_, None, _) =>
        expandCoproductSchema(coprod, referencedSchemas)
      case Primitive(name, format) =>
        Schema.Primitive(name, format, None)
      case Array(elementType) =>
        Schema.Array(toSchema(elementType, coprodBase, referencedSchemas), None)
      case DocumentedEnum(elementType, values) =>
        Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas), values, None)
      case lzy: LazySchema =>
        toSchema(lzy.value, coprodBase, referencedSchemas)
    }
  }

  private def expandRecordSchema(record: DocumentedJsonSchema.DocumentedRecord, coprodBase: Option[DocumentedCoProd], referencedSchemas: Set[String]): Schema = {
    val fieldsSchema = record.fields
      .map(f => Schema.Property(f.name, toSchema(f.tpe, None, referencedSchemas), !f.isOptional, f.documentation))

    val additionalProperties = record.additionalProperties.map(toSchema(_, None, referencedSchemas))

    coprodBase.fold[Schema] {
      Schema.Object(fieldsSchema, additionalProperties, None)
    } { coprod =>
      val discriminatorField =
        Schema.Property(coprod.discriminatorName, Schema.simpleString, isRequired = true, description = None)

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
      coprod.alternatives.map { case (tag, record) => tag -> toSchema(record, Some(coprod), referencedSchemas) }
    Schema.OneOf(coprod.discriminatorName, alternativesSchemas, None)
  }
}
