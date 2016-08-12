package endpoints

import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

trait AssetXhrClient extends AssetAlg with EndpointXhrClient {

  case class AssetInfo(path: String, name: String) // FIXME Better DX
  type Asset = ArrayBuffer

  lazy val assetSegments: Path[AssetInfo] = {
    case AssetInfo(path, name) =>
      val rawPath = s"$path/$name"
      val digest = digests.getOrElse(rawPath, throw new Exception(s"Asset not found: $rawPath"))
      s"$path/${js.URIUtils.encodeURIComponent(name)}-$digest"
  }

  def assetsEndpoint(url: Url[AssetInfo]): Endpoint[AssetInfo, Asset] =
    endpoint(arrayBufferGet(url), arrayBufferResponse)

  private def arrayBufferGet[A](url: Url[A]): Request[A] =
    (a: A) => {
      val xhr = new XMLHttpRequest
      xhr.open("GET", url.encode(a))
      xhr.responseType = "arraybuffer"
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
