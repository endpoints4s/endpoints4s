package endpoints

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.MimeTypes
import play.api.mvc.Results
import play.mvc.Http.HeaderNames

trait AssetsRouting extends AssetsAlg with PlayRouting {

  case class AssetInfo(path: Seq[String], digest: String, name: String)
  type Asset = Option[(Source[ByteString, _], Option[Long], Option[String])]

  lazy val assetSegments: Path[AssetInfo] = {
    val stringPath = segment[String]
    new Path[AssetInfo] {
      def decode(segments: List[String]) =
        segments.reverse match {
          case s :: p =>
            val i = s.lastIndexOf('-')
            if (i > 0) {
              val (name, digest) = s.splitAt(i)
              Some((AssetInfo(p.reverse, digest.drop(1), name), Nil))
            } else None
          case Nil => None
        }
      def encode(s: AssetInfo) =
        s.path.foldRight(stringPath.encode(s"/${s.name}-${s.digest}"))((segment, path) => s"/${stringPath.encode(segment)}$path")
    }
  }

  def assetsEndpoint(url: Url[AssetInfo]): Endpoint[AssetInfo, Asset] =
    endpoint(get(url), assetResponse)

  private def assetResponse: Response[Asset] = {
      case Some((resource, maybeLength, maybeContentType)) =>
        Results.Ok
          .sendEntity(HttpEntity.Streamed(resource, maybeLength, maybeContentType))
          .withHeaders(
            HeaderNames.CONTENT_DISPOSITION -> "inline",
            HeaderNames.CACHE_CONTROL -> "public, max-age=31536000"
          )
      case None => Results.NotFound
    }

  def assetsResources(pathPrefix: Option[String] = None): AssetInfo => Asset =
    assetInfo => {
      if (digests.get(assetInfo.path.mkString("", "/", "/") ++ assetInfo.name).contains(assetInfo.digest)) {
        Option(getClass.getResourceAsStream(pathPrefix.getOrElse("") ++ assetInfo.path.mkString("/", "/", "/") ++ assetInfo.name)).map { stream =>
          (
            StreamConverters.fromInputStream(() => stream),
            Some(stream.available().toLong),
            MimeTypes.forFileName(assetInfo.name).orElse(Some(ContentTypes.BINARY))
          )
        }
      } else None
    }

}
