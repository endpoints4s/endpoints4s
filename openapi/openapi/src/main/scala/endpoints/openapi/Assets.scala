package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model._

/**
  * Interpreter for [[algebra.Assets]]
  *
  * @group interpreters
  */
trait Assets
  extends algebra.Assets
    with EndpointsWithCustomErrors
    with StatusCodes {

  type AssetRequest = Nothing
  type AssetPath = Nothing
  type AssetResponse = Nothing

  def assetSegments(name: String, description: Option[String]): Path[AssetPath] =
    DocumentedUrl(Right(DocumentedParameter(name, required = true, description, Schema.simpleString)) :: Nil, Nil)

  def assetsEndpoint(url: Url[AssetPath], docs: Documentation, notFoundDocs: Documentation): Endpoint[AssetRequest, AssetResponse] =
    endpoint(
      DocumentedRequest(Get, url, emptyHeaders, None, emptyRequest),
      DocumentedResponse(OK, docs.getOrElse(""), emptyResponseHeaders, Map.empty) ::
        DocumentedResponse(NotFound, notFoundDocs.getOrElse(""), emptyResponseHeaders, Map.empty) ::
        Nil
    )

  def digests: Map[String, String] = Map.empty

}
