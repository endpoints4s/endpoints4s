package endpoints4s.http4s.server

import java.net.URL

import cats.effect.{Blocker, ContextShift}
import cats.implicits._
import endpoints4s.algebra.Documentation
import endpoints4s.{Invalid, Valid, algebra}
import fs2.io._
import org.http4s.headers._
import org.http4s._

trait Assets extends algebra.Assets with EndpointsWithCustomErrors {
  val DefaultBufferSize = 10240

  case class AssetRequest(
      assetPath: AssetPath,
      isGzipSupported: Boolean,
      ifModifiedSince: Option[HttpDate]
  )

  case class AssetPath(path: Seq[String], digest: String, name: String)

  sealed trait AssetResponse
  object AssetResponse {
    case object NotFound extends AssetResponse
    case class Found(
        data: fs2.Stream[Effect, Byte],
        contentLength: Long,
        lastModified: Option[HttpDate],
        mediaType: Option[MediaType],
        isGzipped: Boolean,
        expired: Boolean
    ) extends AssetResponse
  }

  override def assetSegments(
      name: String,
      docs: Documentation
  ): Path[AssetPath] =
    (segments: List[String]) =>
      segments.reverse match {
        case s :: p =>
          val i = s.lastIndexOf('-')
          if (i > 0) {
            val (name, digest) = s.splitAt(i)
            Some((Valid(AssetPath(p.reverse, digest.drop(1), name)), Nil))
          } else Some((Invalid("Invalid asset segments"), Nil))
        case Nil => None
      }

  private lazy val gzipSupport: RequestHeaders[Boolean] =
    headers => Valid(headers.get(`Accept-Encoding`).exists(_.satisfiedBy(ContentCoding.gzip)))

  private lazy val ifModifiedSince: RequestHeaders[Option[HttpDate]] =
    headers => Valid(headers.get(`If-Modified-Since`).map(_.date))

  override def assetsEndpoint(
      url: Url[AssetPath],
      docs: Documentation,
      notFoundDocs: Documentation
  ): Endpoint[AssetRequest, AssetResponse] = {
    val assetRequest =
      requestPartialInvariantFunctor
        .xmap(
          request(
            Get,
            url,
            headers = requestHeadersSemigroupal.product(gzipSupport, ifModifiedSince)
          ),
          (t: (AssetPath, Boolean, Option[HttpDate])) => AssetRequest(t._1, t._2, t._3),
          (assetRequest: AssetRequest) =>
            (assetRequest.assetPath, assetRequest.isGzipSupported, assetRequest.ifModifiedSince)
        )

    endpoint(assetRequest, assetResponse)
  }

  private val assetResponse: Response[AssetResponse] = {
    case AssetResponse.NotFound                    => Response(NotFound)
    case AssetResponse.Found(_, _, _, _, _, false) => Response(NotModified)
    case AssetResponse.Found(data, length, lastModified, mediaType, isGzipped, true) =>
      val lastModifiedHeader = lastModified.map(`Last-Modified`(_))
      val contentTypeHeader = mediaType.map(`Content-Type`(_))
      val contentCodingHeader =
        if (isGzipped) Some(`Content-Encoding`(ContentCoding.gzip)) else None
      val contentLengthHeader =
        `Content-Length`.fromLong(length).getOrElse(`Transfer-Encoding`(TransferCoding.chunked))

      val headers = Headers(
        contentLengthHeader :: List(
          contentTypeHeader,
          contentCodingHeader,
          lastModifiedHeader
        ).flatten
      )

      Response(
        headers = headers,
        body = data
      )

  }

  private def toUrl(
      pathPrefix: Option[String],
      assetRequest: AssetRequest
  ): Option[(URL, Boolean)] = {
    val assetInfo = assetRequest.assetPath
    val path =
      if (assetInfo.path.nonEmpty)
        assetInfo.path.mkString("", "/", s"/${assetInfo.name}")
      else assetInfo.name
    val hasDigest = digests.get(path).contains(assetInfo.digest)

    lazy val resourcePath = pathPrefix.getOrElse("") ++ s"/$path"
    def nonGzippedAsset = Option(getClass.getResource(resourcePath)).map((_, false))

    if (hasDigest && assetRequest.isGzipSupported)
      Option(getClass.getResource(s"$resourcePath.gz"))
        .map((_, true))
        .orElse(nonGzippedAsset)
    else if (hasDigest) nonGzippedAsset
    else None
  }

  private def toMediaType(name: String): Option[MediaType] =
    name.lastIndexOf('.') match {
      case -1 => None
      case i  => MediaType.forExtension(name.substring(i + 1))
    }

  def assetsResources(
      pathPrefix: Option[String] = None,
      blocker: Blocker
  )(implicit cs: ContextShift[Effect]): AssetRequest => AssetResponse =
    assetRequest =>
      toUrl(pathPrefix, assetRequest)
        .map {
          case (url, isGzipped) =>
            val urlConn = url.openConnection

            val ifModifiedSince = assetRequest.ifModifiedSince
            val lastModified = HttpDate.fromEpochSecond(urlConn.getLastModified / 1000).toOption
            val expired = (ifModifiedSince, lastModified).mapN(_ < _).getOrElse(true)
            val contentLength = urlConn.getContentLengthLong
            val mediaType = toMediaType(assetRequest.assetPath.name)
            val data =
              readInputStream[Effect](Effect.delay(url.openStream), DefaultBufferSize, blocker)

            AssetResponse.Found(
              data,
              contentLength,
              lastModified,
              mediaType,
              isGzipped,
              expired
            )
        }
        .getOrElse(AssetResponse.NotFound)
}
