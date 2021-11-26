package endpoints4s.algebra

import server.ServerAssets
trait AssetsTestApi extends EndpointsTestApi with ServerAssets {

  val assetEndpoint: Endpoint[AssetRequest, AssetResponse] =
    assetsEndpoint(path / "assets" / assetSegments())

}
