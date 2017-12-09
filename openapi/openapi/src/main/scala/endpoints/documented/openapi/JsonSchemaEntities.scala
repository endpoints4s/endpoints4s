package endpoints
package documented
package openapi

import endpoints.documented.openapi.model.{MediaType, Schema}

import scala.language.higherKinds

/**
  * Interpreter for [[algebra.JsonSchemaEntities]] that produces a documentation of the JSON schemas.
  */
trait JsonSchemaEntities
  extends algebra.JsonSchemaEntities
    with Endpoints
    with JsonSchemas {

  def jsonRequest[A](documentation: Option[String])(implicit codec: JsonSchema[A]): Option[DocumentedRequestEntity] =
    Some(DocumentedRequestEntity(documentation, Map("application/json" -> MediaType(Some(toSchema(codec))))))

  def jsonResponse[A](documentation: String)(implicit codec: JsonSchema[A]): List[DocumentedResponse] =
    DocumentedResponse(200, documentation, Map("application/json" -> MediaType(Some(toSchema(codec))))) :: Nil

  def toSchema(documentedCodec: DocumentedJsonSchema): Schema = {
    import DocumentedJsonSchema._
    documentedCodec match {
      case DocumentedRecord(fields) =>
        val fieldsSchema =
          fields.map(f => Schema.Property(f.name, toSchema(f.tpe), f.isOptional))
        Schema.Object(fieldsSchema, None)
      case DocumentedCoProd(alternatives) =>
        val alternativesSchemas =
          alternatives.map { case (tag, record) =>
            Schema.Object(Schema.Property(tag, toSchema(record), isRequired = true) :: Nil, None)
          }
        Schema.OneOf(alternativesSchemas, None)
      case Primitive(name) => Schema.Primitive(name)
      case Array(elementType) => Schema.Array(toSchema(elementType))
    }
  }

}
