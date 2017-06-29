package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.Assets]] that ignores information related
  * to documentation and delegates to another [[endpoints.algebra.Assets]] interpreter.
  */
trait Assets
  extends algebra.Assets
    with Endpoints {

  val delegate: endpoints.algebra.Assets

  type AssetRequest = delegate.AssetRequest
  type AssetPath = delegate.AssetPath
  type AssetResponse = delegate.AssetResponse

  def assetSegments(name: String): Path[AssetPath] = delegate.assetSegments

  def assetsEndpoint(url: Url[AssetPath], description: String, notFoundDescription: String): Endpoint[AssetRequest, AssetResponse] =
    delegate.assetsEndpoint(url)

  def digests: Map[String, String] = delegate.digests

}
