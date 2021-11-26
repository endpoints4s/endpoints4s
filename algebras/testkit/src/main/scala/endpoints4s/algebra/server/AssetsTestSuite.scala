package endpoints4s.algebra.server

import akka.http.scaladsl.model.HttpMethods.{GET}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{`Last-Modified`, `Content-Type`, `Content-Encoding`}
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.headers.HttpEncodings

trait AssetsTestSuite[T <: endpoints4s.algebra.AssetsTestApi] extends ServerAssetTest[T] {

  "Assets interpreter" should {
    "respond OK for found asset" in {
      val assetResponse = serverApi.foundAssetResponse(
        content = serverApi.noopAssetContent,
        contentLength = 0,
        fileName = "",
        isGzipped = false,
        isExpired = true,
        lastModifiedSeconds = 0
      )
      serveAssetsEndpoint(serverApi.assetEndpoint, assetResponse) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset.txt")
        whenReady(send(request)) { case (response, entity) =>
          assert(response.status.intValue() == 200)
          ()
        }
      }
    }

    "respond NotFound for not found asset" in {
      val assetResponse = serverApi.notFoundAssetResponse
      serveAssetsEndpoint(serverApi.assetEndpoint, assetResponse) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset.txt")
        whenReady(send(request)) { case (response, entity) =>
          assert(response.status.intValue() == 404)
          ()
        }
      }
    }

    "respond with Content-Length header" in {
      val contentLength = 0L
      val assetResponse = serverApi.foundAssetResponse(
        content = serverApi.noopAssetContent,
        contentLength = contentLength,
        fileName = "file.txt",
        isGzipped = false,
        isExpired = true,
        lastModifiedSeconds = 0
      )
      serveAssetsEndpoint(serverApi.assetEndpoint, assetResponse) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset.txt")
        whenReady(send(request)) { case (response, entity) =>
          assert(
            response.entity.contentLengthOption.contains(contentLength)
          )
          ()
        }
      }
    }

    "infer and respond with Content-Type header" in {
      val assetResponse = serverApi.foundAssetResponse(
        content = serverApi.noopAssetContent,
        contentLength = 0,
        fileName = "file.txt",
        isGzipped = false,
        isExpired = true,
        lastModifiedSeconds = 0
      )
      serveAssetsEndpoint(serverApi.assetEndpoint, assetResponse) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset.txt")
        whenReady(send(request)) { case (response, entity) =>
          assert(
            response
              .header[`Content-Type`]
              .contains(
                `Content-Type`(
                  ContentType.WithMissingCharset(MediaTypes.`text/plain`)
                )
              )
          )
          ()
        }
      }
    }

    "respond with gzip header for gzipped files" in {
      val assetResponse = serverApi.foundAssetResponse(
        content = serverApi.noopAssetContent,
        contentLength = 0,
        fileName = "file.txt",
        isGzipped = true,
        isExpired = true,
        lastModifiedSeconds = 0
      )
      serveAssetsEndpoint(serverApi.assetEndpoint, assetResponse) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset.txt")

        whenReady(send(request)) { case (response, entity) =>
          assert(
            response
              .header[`Content-Encoding`]
              .contains(`Content-Encoding`(HttpEncodings.gzip))
          )
          ()
        }
      }
    }

    "respond NotModified for not expired asset" in {
      val assetResponse = serverApi.foundAssetResponse(
        content = serverApi.noopAssetContent,
        contentLength = 0,
        fileName = "",
        isGzipped = false,
        isExpired = false,
        lastModifiedSeconds = 0
      )
      serveAssetsEndpoint(serverApi.assetEndpoint, assetResponse) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset.txt")
        whenReady(send(request)) { case (response, entity) =>
          assert(response.status.intValue() == 304)
          ()
        }
      }
    }

    "respond with Last-Modified header (rfc7232)" in {
      val lastModifiedSeconds = 10L
      val assetResponse = serverApi.foundAssetResponse(
        content = serverApi.noopAssetContent,
        contentLength = 0,
        fileName = "",
        isGzipped = false,
        isExpired = true,
        lastModifiedSeconds = lastModifiedSeconds
      )
      serveAssetsEndpoint(serverApi.assetEndpoint, assetResponse) { port =>
        val request =
          HttpRequest(method = GET, uri = s"http://localhost:$port/assets/asset.txt")
        whenReady(send(request)) { case (response, entity) =>
          assert(
            response
              .header[`Last-Modified`]
              .contains(`Last-Modified`(DateTime(lastModifiedSeconds * 1000)))
          )
          ()
        }
      }
    }
  }

}
