package endpoints
package openapi

import endpoints.openapi.model.MediaType

/**
  * Partial interpreter for [[algebra.JsonEntities]].
  *
  * This interpreter documents that entities have a JSON content type, but
  * it can not document the schemas of these entities. See [[algebra.JsonSchemaEntities]]
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
