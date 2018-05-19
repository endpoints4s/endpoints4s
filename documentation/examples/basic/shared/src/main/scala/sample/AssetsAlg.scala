package sample

import endpoints.algebra._

trait AssetsAlg extends Endpoints with Assets {

  lazy val digests = AssetsDigests.digests

  val assets =
    assetsEndpoint(path / "assets" / assetSegments("assetPath", Some("Serves static assets")))


}
