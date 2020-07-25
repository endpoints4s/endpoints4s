package endpoints4s.algebra.server

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpRequest

trait AssetsTestSuite[T <: endpoints4s.algebra.AssetsTestApi] extends ServerAssetTest[T] {

  "Assets interpreter" should {
    "respond OK for found asset" in {
      serveAssetsEndpointFromPath(serverApi.assetEndpoint, Some("/assets")) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset1.txt")

        whenReady(send(request)) {
          case (response, entity) =>
            assert(response.status.intValue() == 200)
            ()
        }
      }
    }

    "respond NotFound for not found asset" in {
      serveAssetsEndpointFromPath(serverApi.assetEndpoint, Some("/assets")) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/non-existing-asset.txt")

        whenReady(send(request)) {
          case (response, entity) =>
            assert(response.status.intValue() == 404)
            ()
        }
      }
    }
  }

}
