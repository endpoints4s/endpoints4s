package endpoints.algebra

/**
  * Partially applies the [[endpoints.algebra.JsonEntities]] algebra interface to fix the
  * `JsonRequest` and `JsonResponse` types to be `JsonSchema`.
  *
  * @group algebras
  */
trait JsonSchemaEntities
  extends endpoints.algebra.JsonEntities
    with JsonSchemas
    with JsonCodecs {

//#type-carrier
  type JsonCodec[A] = JsonSchema[A]
//#type-carrier

}
