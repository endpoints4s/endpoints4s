package endpoints4s.algebra

trait AssetsTestApi extends EndpointsTestApi with ServerAssets {

  val assetEndpoint: Endpoint[AssetRequest, AssetResponse] =
    assetsEndpoint(path / "assets" / assetSegments())

}
