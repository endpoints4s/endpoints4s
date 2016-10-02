package endpoints

import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

/**
  * Client that relies on the web browser to handle gzip compression
  */
trait AssetXhrClient extends AssetAlg with EndpointXhrClient {

  case class AssetPath(path: String, name: String)

  /**
    * As a client, we just need to give the path of the asset we are interested in, the web browser will
    * automatically set HTTP headers to handle gzip compression (`Accept-Encoding`) and decompress the response.
    */
  type AssetRequest = AssetPath
  type AssetResponse = ArrayBuffer

  def asset(path: String, name: String): AssetRequest = AssetPath(path, name)

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
      xhr.open("GET", url.encode(assetRequest))
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
