package endpoints
package documented
package algebra

/**
  * Partially applies the [[JsonEntities]] algebra interface to fix the
  * `JsonRequest` and `JsonResponse` types to be `JsonSchema`.
  */
trait JsonSchemaEntities
  extends JsonEntities
    with JsonSchemas {

  type JsonRequest[A] = JsonSchema[A]
  type JsonResponse[A] = JsonSchema[A]

}
