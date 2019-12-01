package endpoints.algebra

/**
  * Partially applies the [[JsonEntities]] algebra interface to fix the
  * `JsonRequest` and `JsonResponse` types to be `JsonSchema`.
  *
  * @group algebras
  */
trait JsonSchemaEntities extends JsonCodecs with JsonSchemas {

//#type-carrier
  type JsonCodec[A] = JsonSchema[A]
//#type-carrier

}
