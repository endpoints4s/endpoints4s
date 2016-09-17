package endpoints

import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

trait AssetXhrClient extends AssetAlg with EndpointXhrClient {

  case class AssetRequest(assetInfo: AssetPath, acceptGzip: Boolean)
  case class AssetPath(path: String, name: String)
  type AssetResponse = ArrayBuffer

  def asset(path: String, name: String): AssetRequest =
    AssetRequest(
      AssetPath(path, name),
      acceptGzip = true // HACK Assumes that all browsers support gzip
    )

  lazy val assetSegments: Path[AssetPath] = {
    case AssetPath(path, name) =>
      val rawPath = s"$path/$name"
      val digest = digests.getOrElse(rawPath, throw new Exception(s"Asset not found: $rawPath"))
      s"$path/${js.URIUtils.encodeURIComponent(name)}-$digest"
  }

  def assetsEndpoint(url: Url[AssetPath]): Endpoint[AssetRequest, AssetResponse] =
    endpoint(arrayBufferGet(url), arrayBufferResponse)

  private def arrayBufferGet(url: Url[AssetPath]): Request[AssetRequest] =
    (assetRequest: AssetRequest) => {
      val xhr = new XMLHttpRequest
      xhr.open("GET", url.encode(assetRequest.assetInfo))
      xhr.responseType = "arraybuffer"
      // HACK No need to set the Accept-Encoding header because it is automatically set by the browser
      (xhr, None)
    }

  private def arrayBufferResponse: Response[ArrayBuffer] =
    (xhr: XMLHttpRequest) => {
      if (xhr.status < 300) {
        try {
          Right(xhr.response.asInstanceOf[ArrayBuffer])
        } catch {
          case exn: Exception => Left(exn)
        }
      } else Left(new Exception("Resource not found"))
    }

}
