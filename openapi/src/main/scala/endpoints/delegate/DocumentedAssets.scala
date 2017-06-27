package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedAssets]] that ignores information related
  * to documentation and delegates to another [[algebra.Assets]] interpreter.
  */
trait DocumentedAssets
  extends algebra.DocumentedAssets
    with DocumentedEndpoints {

  val delegate: algebra.Assets

  type AssetRequest = delegate.AssetRequest
  type AssetPath = delegate.AssetPath
  type AssetResponse = delegate.AssetResponse

  def assetSegments(name: String): Path[AssetPath] = delegate.assetSegments

  def assetsEndpoint(url: Url[AssetPath], description: String, notFoundDescription: String): Endpoint[AssetRequest, AssetResponse] =
    delegate.assetsEndpoint(url)

  def digests: Map[String, String] = delegate.digests

}
