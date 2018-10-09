package endpoints.algebra

/**
  * Describes endpoints related to static assets (e.g. medias, scripts, stylesheets, etc.)
  *
  * @group algebras
  */
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
  def assetSegments(name: String = "", docs: Documentation = None): Path[AssetPath]

  /**
    * @param url URL description
    * @param docs description of a response when asset is found. Required by openapi
    * @param notFoundDocs description of a not found asset response. Required by openapi
    * @return An HTTP endpoint serving assets
    */
  def assetsEndpoint(url: Url[AssetPath], docs: Documentation = None, notFoundDocs: Documentation = None): Endpoint[AssetRequest, AssetResponse]

  /** The digests of the assets */
  def digests: Map[String, String]

}