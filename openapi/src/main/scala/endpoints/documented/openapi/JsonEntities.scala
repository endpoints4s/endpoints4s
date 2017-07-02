package endpoints.documented
package openapi

/**
  * Partial interpreter for [[algebra.JsonEntities]].
  */
trait JsonEntities
  extends algebra.JsonEntities
    with Endpoints {

  def jsonRequest[A : JsonRequest](description: Option[String]): RequestEntity[A] =
    Some(DocumentedRequestEntity(description, Map("application/json" -> MediaType(None))))

  def jsonResponse[A : JsonResponse](description: String): Response[A] =
    DocumentedResponse(200, description, Map("application/json" -> MediaType(None))) :: Nil

}
