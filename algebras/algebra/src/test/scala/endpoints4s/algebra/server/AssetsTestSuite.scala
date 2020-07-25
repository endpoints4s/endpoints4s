package endpoints4s.algebra.server

import akka.http.scaladsl.model.HttpMethods.{GET}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{`Last-Modified`, `If-Modified-Since`, `Content-Type`}
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaTypes

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

    "send Last-Modified header (rfc7232)" in {
      serveAssetsEndpointFromPath(serverApi.assetEndpoint, Some("/assets")) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset1.txt")

        whenReady(send(request)) {
          case (response, entity) =>
            assert(
              response
                .header[`Last-Modified`]
                .nonEmpty
            )
            ()
        }
      }
    }

    "evaluate If-Modified-Since header (rfc7232)" in {
      serveAssetsEndpointFromPath(serverApi.assetEndpoint, Some("/assets")) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset1.txt").withHeaders(
            `If-Modified-Since`(DateTime.apply(2020, 7, 26))
          )

        whenReady(send(request)) {
          case (response, entity) =>
            assert(response.status.intValue() == 304)
            ()
        }
      }
    }

    "send correct Content-Type header" in {
      serveAssetsEndpointFromPath(serverApi.assetEndpoint, Some("/assets")) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset1.txt")

        whenReady(send(request)) {
          case (response, entity) =>
            assert(
              response
                .header[`Content-Type`]
                .contains(`Content-Type`(ContentType.WithMissingCharset(MediaTypes.`text/plain`)))
            )
            ()
        }
      }
    }
  }

}
