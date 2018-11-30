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
    DocumentedResponse(200, docs.getOrElse(""), Map("application/json" -> MediaType(Some(toSchema(codec))))) :: Nil

  def toSchema(jsonSchema: DocumentedJsonSchema): Schema =
    toSchema(jsonSchema, None, Set.empty)

  private def toSchema(documentedCodec: DocumentedJsonSchema, coprodBase: Option[DocumentedCoProd], referencedSchemas: Set[String]): Schema = {
    documentedCodec match {
      case record @ DocumentedRecord(_, Some(name)) =>
        if (referencedSchemas(name)) Schema.Reference(name, None)
        else Schema.Reference(name, Some(expandRecordSchema(record, coprodBase, referencedSchemas + name)))
      case record @ DocumentedRecord(_, None) =>
        expandRecordSchema(record, None, referencedSchemas)
      case coprod @ DocumentedCoProd(_, Some(name), _) =>
        if (referencedSchemas(name)) Schema.Reference(name, None)
        else Schema.Reference(name, Some(expandCoproductSchema(coprod, referencedSchemas + name)))
      case coprod @ DocumentedCoProd(_, None, _) =>
        expandCoproductSchema(coprod, referencedSchemas)
      case Primitive(name, format) =>
        Schema.Primitive(name, format)
      case Array(elementType) =>
        Schema.Array(toSchema(elementType, coprodBase, referencedSchemas))
      case DocumentedEnum(elementType, values) =>
        Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas), values)
      case lzy: LazySchema =>
        toSchema(lzy.value, coprodBase, referencedSchemas)
    }
  }

  private def expandRecordSchema(record: DocumentedJsonSchema.DocumentedRecord, coprodBase: Option[DocumentedCoProd], referencedSchemas: Set[String]): Schema = {
    val fieldsSchema = record.fields
      .map(f => Schema.Property(f.name, toSchema(f.tpe, None, referencedSchemas), !f.isOptional, f.documentation))

    coprodBase.fold[Schema] {
      Schema.Object(fieldsSchema, None)
    } { coprod =>
      val discriminatorField =
        Schema.Property(coprod.discriminatorName, Schema.simpleString, isRequired = true, description = None)

      coprod.name.fold[Schema] {
        Schema.Object(discriminatorField :: fieldsSchema, None)
      } { coproductName =>
        Schema.AllOf(
          schemas = List(
            Schema.Reference(coproductName, None),
            Schema.Object(discriminatorField :: fieldsSchema, None)
          )
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
