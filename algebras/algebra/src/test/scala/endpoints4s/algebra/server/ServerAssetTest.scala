package endpoints4s.algebra.server

import endpoints4s.algebra

trait ServerAssetTest[T <: algebra.Endpoints with algebra.Assets] extends ServerTestBase[T] {

  def serveAssetsEndpointFromPath(
      endpoint: serverApi.Endpoint[serverApi.AssetRequest, serverApi.AssetResponse],
      pathPrefix: Option[String]
  )(runTests: Int => Unit): Unit

}
