package endpoints4s.xhr

import endpoints4s.algebra
import endpoints4s.algebra.Documentation
import org.scalajs.dom.XMLHttpRequest

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

/** Client that relies on the web browser to handle gzip compression
  *
  * @group interpreters
  */
trait Assets extends algebra.Assets with EndpointsWithCustomErrors {

  /** As a client, we just need to give the path of the asset we are interested in, the web browser will
    * automatically set HTTP headers to handle gzip compression (`Accept-Encoding`) and decompress the response.
    */
  type AssetRequest = AssetPath

  /** {{{
    *   // foo/bar/baz-123abc
    *   AssetPath("foo/bar", "baz")
    * }}}
    */
  case class AssetPath(path: String, name: String)

  /** As we request the asset via an XMLHttpRequest, we get its content as an
    * `ArrayBuffer`
    */
  type AssetResponse = ArrayBuffer

  /** Convenient constructor for building an [[AssetRequest]] from its path and name.
    *
    * {{{
    *   myAssetsEndpoint(asset("foo/bar", "baz"))
    * }}}
    */
  def asset(path: String, name: String): AssetRequest = AssetPath(path, name)

  /** Encodes an [[AssetPath]] as a request path.
    * Throws an exception if the asset digest is not found.
    */
  // FIXME Check the asset digest in the `asset` smart constructor
  def assetSegments(name: String, docs: Documentation): Path[AssetPath] = {
    case AssetPath(path, name) =>
      val rawPath = s"$path/$name"
      val digest = digests.getOrElse(
        rawPath,
        throw new Exception(s"Asset not found: $rawPath")
      )
      s"$path/${js.URIUtils.encodeURIComponent(name)}-$digest"
  }

  /** An endpoint for requesting assets.
    *
    * If the server fails to find the requested asset, this endpoint returns
    * a failed response.
    *
    * @param url URL description
    * @return An HTTP endpoint for requesting assets
    */
  def assetsEndpoint(
      url: Url[AssetPath],
      docs: Documentation,
      notFoundDocs: Documentation
  ): Endpoint[AssetRequest, AssetResponse] =
    endpoint(arrayBufferGet(url), arrayBufferResponse)

  private def arrayBufferGet(url: Url[AssetPath]): Request[AssetRequest] =
    new Request[AssetRequest] {
      def apply(
          assetRequest: AssetRequest
      ): (XMLHttpRequest, Option[js.Any]) = {
        val xhr = new XMLHttpRequest
        xhr.open("GET", url.encode(assetRequest))
        xhr.responseType = "arraybuffer"
        (xhr, None)
      }
      def href(assetRequest: AssetRequest): String = url.encode(assetRequest)
    }

  private def arrayBufferResponse: Response[ArrayBuffer] =
    ok { xhr =>
      try {
        Right(xhr.response.asInstanceOf[ArrayBuffer])
      } catch {
        case exn: Exception => Left(exn)
      }
    }

}
