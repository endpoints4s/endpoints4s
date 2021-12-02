package endpoints4s.algebra.server

import endpoints4s.algebra

trait ServerAssetTest[T <: algebra.Endpoints with ServerAssets] extends ServerTestBase[T] {

  def serveAssetsEndpoint(
      endpoint: serverApi.Endpoint[serverApi.AssetRequest, serverApi.AssetResponse],
      response: => serverApi.AssetResponse
  )(runTests: Int => Unit): Unit

}
