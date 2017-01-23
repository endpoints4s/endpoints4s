package sample

import endpoints.algebra._

/**
  * Created by wpitula on 1/23/17.
  */
trait AssetsAlg extends Endpoints with Assets {

  lazy val digests = AssetsDigests.digests

  val assets =
    assetsEndpoint(path / "assets" / assetSegments)

}
