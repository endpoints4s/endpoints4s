package endpoints.algebra

/** Describes endpoints related to static assets (e.g. medias, scripts, stylesheets, etc.) */
trait Assets extends Endpoints {

  /** An HTTP request to retrieve an asset */
  type AssetRequest
  /** The path of the asset */
  type AssetPath
  /** An HTTP response containing an asset */
  type AssetResponse

  /**
    * A [[Path]] that extracts an [[AssetPath]] from all the path segments.
    *
    * Consider the following definition:
    * {{{
    *   val assets = assetsEndpoint(get(path / "assets" / assetsSegments))
    * }}}
    *
    * Then, here is how the following requests are decoded:
    * - `/assets/foo` => `foo`
    * - `/assets/foo/bar` => `foo/bar`
    */
  def assetSegments(name: String = ""): Path[AssetPath]

  /**
    * @param url URL description
    * @return An HTTP endpoint serving assets
    */
  def assetsEndpoint(url: Url[AssetPath], documentation: String = "", notFoundDocumentation: String = ""): Endpoint[AssetRequest, AssetResponse]

  /** The digests of the assets */
  def digests: Map[String, String]

}