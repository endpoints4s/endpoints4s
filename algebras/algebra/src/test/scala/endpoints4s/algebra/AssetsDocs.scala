package endpoints4s.algebra

trait AssetsDocs extends Assets {

  //#assets-endpoint
  val assets: Endpoint[AssetRequest, AssetResponse] =
    assetsEndpoint(path / "assets" / assetSegments())
  //#assets-endpoint

  //#digests
  val digests = Map("main.css" -> "2018-10-09T14:32:12Z")
  //#digests

}
