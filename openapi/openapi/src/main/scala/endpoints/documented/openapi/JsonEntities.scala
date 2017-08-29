package endpoints.documented
package openapi

import endpoints.documented.openapi.model.MediaType

/**
  * Partial interpreter for [[algebra.JsonEntities]].
  */
trait JsonEntities
  extends algebra.JsonEntities
    with Endpoints {

  def jsonRequest[A : JsonRequest](documentation: Option[String]): RequestEntity[A] =
    Some(DocumentedRequestEntity(documentation, Map("application/json" -> MediaType(None))))

  def jsonResponse[A : JsonResponse](documentation: String): Response[A] =
    DocumentedResponse(200, documentation, Map("application/json" -> MediaType(None))) :: Nil

}
