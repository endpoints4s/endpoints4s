package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model.{MediaType, Schema}

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that produces a documentation of the JSON schemas.
  */
trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with Endpoints
    with JsonSchemas {

  def jsonRequest[A](docs: Documentation)(implicit codec: JsonSchema[A]): Option[DocumentedRequestEntity] =
    Some(DocumentedRequestEntity(docs, Map("application/json" -> MediaType(Some(toSchema(codec))))))

  def jsonResponse[A](docs: Documentation)(implicit codec: JsonSchema[A]): List[DocumentedResponse] =
    DocumentedResponse(200, docs.getOrElse(""), Map("application/json" -> MediaType(Some(toSchema(codec))))) :: Nil

  def toSchema(documentedCodec: DocumentedJsonSchema, coprodName: Option[String] = None): Schema = {
    import DocumentedJsonSchema._

    documentedCodec match {
      case record @ DocumentedRecord(_, Some(name)) =>
        Schema.Reference(name, Some(expandRecordSchema(record, coprodName)))
      case record @ DocumentedRecord(_, None) =>
        expandRecordSchema(record)
      case coprod @ DocumentedCoProd(_, Some(name)) =>
        Schema.Reference(name, Some(expandCoproductSchema(coprod)))
      case coprod @ DocumentedCoProd(_, None) =>
        expandCoproductSchema(coprod)
      case Primitive(name, format) =>
        Schema.Primitive(name, format)
      case Array(elementType) =>
        Schema.Array(toSchema(elementType))
    }
  }

  private def expandRecordSchema(record: DocumentedJsonSchema.DocumentedRecord, coprodName: Option[String] = None): Schema = {
    val fieldsSchema = record.fields
      .map(f => Schema.Property(f.name, toSchema(f.tpe), !f.isOptional, f.documentation))

    coprodName.fold[Schema] {
      Schema.Object(fieldsSchema, None)
    } { coproductName =>
      val discriminatorField =
        Schema.Property(discriminatorName, Schema.simpleString, isRequired = true, description = None)
      Schema.AllOf(
        schemas = List(
          Schema.Reference(coproductName, None),
          Schema.Object(discriminatorField :: fieldsSchema, None)
        )
      )
    }
  }

  private def expandCoproductSchema(coprod: DocumentedJsonSchema.DocumentedCoProd): Schema = {
    val alternativesSchemas = coprod.alternatives.map { case (tag, record) => tag -> toSchema(record, coprod.name) }
    Schema.OneOf(discriminatorName, alternativesSchemas, None)
  }
}
