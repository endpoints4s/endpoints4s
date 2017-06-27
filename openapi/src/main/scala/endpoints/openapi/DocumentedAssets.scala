package endpoints
package openapi

/**
  * Interpreter for [[algebra.DocumentedAssets]]
  */
trait DocumentedAssets
  extends algebra.DocumentedAssets
    with DocumentedEndpoints {

  type AssetRequest = Nothing
  type AssetPath = Nothing
  type AssetResponse = Nothing

  def assetSegments(name: String): Path[AssetPath] =
    DocumentedUrl(s"{$name}", List(DocumentedParameter(name, required = true)), Nil)

  def assetsEndpoint(url: Url[AssetPath], description: String, notFoundDescription: String): Endpoint[AssetRequest, AssetResponse] =
    endpoint(
      DocumentedRequest(Get, url, emptyHeaders, emptyRequest),
      DocumentedResponse(200, description, Map.empty) ::
      DocumentedResponse(404, notFoundDescription, Map.empty) ::
      Nil
    )

  def digests: Map[String, String] = Map.empty

}
