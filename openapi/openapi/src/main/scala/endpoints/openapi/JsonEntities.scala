package endpoints.openapi

import endpoints.openapi.model.{MediaType, Schema}
import endpoints.algebra

/**
  * Partial interpreter for [[algebra.JsonEntities]].
  *
  * This interpreter documents that entities have a JSON content type, but
  * it can not document the schemas of these entities. See [[algebra.JsonEntitiesFromSchemas]]
  * for this purpose.
  *
  * @group interpreters
  */
trait JsonEntities
  extends algebra.JsonEntities
    with EndpointsWithCustomErrors {

  def jsonRequest[A : JsonRequest]: RequestEntity[A] =
    Map("application/json" -> MediaType(None))

  def jsonResponse[A : JsonResponse]: ResponseEntity[A] =
    Map("application/json" -> MediaType(None))

}

/**
  * Interpreter for [[algebra.JsonEntitiesFromSchemas]] that produces a documentation of the JSON schemas.
  *
  * @group interpreters
  */
trait JsonEntitiesFromSchemas
  extends algebra.JsonEntitiesFromSchemas
    with EndpointsWithCustomErrors
    with JsonSchemas {

  import DocumentedJsonSchema._

  def jsonRequest[A](implicit codec: JsonSchema[A]) =
    Map("application/json" -> MediaType(Some(toSchema(codec.docs))))

  def jsonResponse[A](implicit codec: JsonSchema[A]) =
    Map("application/json" -> MediaType(Some(toSchema(codec.docs))))

  def toSchema(jsonSchema: DocumentedJsonSchema): Schema =
    toSchema(jsonSchema, None, Set.empty)

  private def toSchema(documentedCodec: DocumentedJsonSchema, coprodBase: Option[(String, DocumentedCoProd)], referencedSchemas: Set[String]): Schema = {
    documentedCodec match {
      case record @ DocumentedRecord(_, _, Some(name), _) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(expandRecordSchema(record, coprodBase, referencedSchemas + name)), None)
      case record @ DocumentedRecord(_, _, None, _) =>
        expandRecordSchema(record, coprodBase, referencedSchemas)
      case coprod @ DocumentedCoProd(_, Some(name), _, _) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(expandCoproductSchema(coprod, referencedSchemas + name)), None)
      case coprod @ DocumentedCoProd(_, None, _, _) =>
        expandCoproductSchema(coprod, referencedSchemas)
      case Primitive(name, format, example) =>
        Schema.Primitive(name, format, None, example)
      case Array(Left(elementType), example) =>
        Schema.Array(Left(toSchema(elementType, coprodBase, referencedSchemas)), None, example)
      case Array(Right(elementTypes), example) =>
        Schema.Array(Right(elementTypes.map(elementType => toSchema(elementType, coprodBase, referencedSchemas))), None, example)
      case DocumentedEnum(elementType, values, Some(name), example) =>
        if (referencedSchemas(name)) Schema.Reference(name, None, None)
        else Schema.Reference(name, Some(Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas + name), values, None, example)), None)
      case DocumentedEnum(elementType, values, None, example) =>
        Schema.Enum(toSchema(elementType, coprodBase, referencedSchemas), values, None, example)
      case lzy: LazySchema =>
        toSchema(lzy.value, coprodBase, referencedSchemas)
    }
  }

  private def expandRecordSchema(record: DocumentedJsonSchema.DocumentedRecord, coprodBase: Option[(String, DocumentedCoProd)], referencedSchemas: Set[String]): Schema = {
    val fieldsSchema = record.fields
      .map(f => Schema.Property(f.name, toSchema(f.tpe, None, referencedSchemas), !f.isOptional, f.documentation))

    val additionalProperties = record.additionalProperties.map(toSchema(_, None, referencedSchemas))

    coprodBase.fold[Schema] {
      Schema.Object(fieldsSchema, additionalProperties, None, record.example)
    } { case (tag, coprod) =>
      val discriminatorField =
        Schema.Property(
          coprod.discriminatorName,
          Schema.Enum(Schema.simpleString, List(tag), None, Some(ujson.Str(tag))),
          isRequired = true,
          description = None
        )

      coprod.name.fold[Schema] {
        Schema.Object(discriminatorField :: fieldsSchema, additionalProperties, None, record.example)
      } { coproductName =>
        Schema.AllOf(
          schemas = List(
            Schema.Reference(coproductName, None, None),
            Schema.Object(discriminatorField :: fieldsSchema, additionalProperties, None, None)
          ),
          description = None,
          record.example
        )
      }
    }
  }

  private def expandCoproductSchema(coprod: DocumentedJsonSchema.DocumentedCoProd, referencedSchemas: Set[String]): Schema = {
    val alternativesSchemas =
      coprod.alternatives.map { case (tag, record) => tag -> toSchema(record, Some(tag -> coprod), referencedSchemas) }
    Schema.OneOf(coprod.discriminatorName, alternativesSchemas, None, coprod.example)
  }
}
