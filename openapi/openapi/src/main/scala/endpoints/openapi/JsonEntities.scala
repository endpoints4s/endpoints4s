package endpoints
package openapi

import endpoints.openapi.model.MediaType
import endpoints.algebra
import endpoints.algebra.Documentation

/**
  * Partial interpreter for [[algebra.JsonEntities]].
  *
  * @group interpreters
  */
trait JsonEntities
  extends algebra.JsonEntities
    with Endpoints {

  def jsonRequest[A : JsonRequest](docs: Documentation): RequestEntity[A] =
    Some(DocumentedRequestEntity(docs, Map("application/json" -> MediaType(None))))

  def jsonResponse[A : JsonResponse](docs: Documentation): Response[A] =
    DocumentedResponse(200, docs.getOrElse(""), Map("application/json" -> MediaType(None))) :: Nil

}
