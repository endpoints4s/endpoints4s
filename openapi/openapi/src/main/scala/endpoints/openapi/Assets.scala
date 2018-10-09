package endpoints
package openapi

import endpoints.algebra
import endpoints.algebra.Documentation
import endpoints.openapi.model._

/**
  * Interpreter for [[algebra.Assets]]
  *
  * @group interpreters
  */
trait Assets
  extends algebra.Assets
    with Endpoints {

  type AssetRequest = Nothing
  type AssetPath = Nothing
  type AssetResponse = Nothing

  def assetSegments(name: String, description: Option[String]): Path[AssetPath] =
    DocumentedUrl(Right(DocumentedParameter(name, required = true, description, Schema.simpleString)) :: Nil, Nil)

  def assetsEndpoint(url: Url[AssetPath], docs: Documentation, notFoundDocs: Documentation): Endpoint[AssetRequest, AssetResponse] =
    endpoint(
      DocumentedRequest(Get, url, emptyHeaders, emptyRequest),
      DocumentedResponse(200, docs.getOrElse(""), Map.empty) ::
        DocumentedResponse(404, notFoundDocs.getOrElse(""), Map.empty) ::
        Nil
    )

  def digests: Map[String, String] = Map.empty

}
