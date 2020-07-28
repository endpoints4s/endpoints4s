package endpoints4s.http4s.server

import java.net.URL

import cats.effect.{Blocker, ContextShift}
import endpoints4s.algebra.Documentation
import endpoints4s.{Valid, algebra}
import fs2.io._
import org.http4s.headers._
import org.http4s._

trait Assets extends algebra.ServerAssets with EndpointsWithCustomErrors {
  val DefaultBufferSize = 10240

  case class AssetRequest(
      assetPath: AssetPath,
      isGzipSupported: Boolean,
      ifModifiedSince: Option[HttpDate]
  )

  case class AssetPath(path: Seq[String], digest: Option[String], name: String)

  sealed trait AssetResponse
  object AssetResponse {
    case object NotFound extends AssetResponse
    case class Found(
        data: fs2.Stream[Effect, Byte],
        contentLength: Long,
        lastModified: Option[HttpDate],
        mediaType: Option[MediaType],
        isGzipped: Boolean,
        isExpired: Boolean
    ) extends AssetResponse
  }

  type AssetContent = fs2.Stream[Effect, Byte]

  def notFoundAssetResponse = AssetResponse.NotFound

  def foundAssetResponse(
      content: AssetContent,
      contentLength: Long,
      fileName: String,
      isGzipped: Boolean,
      isExpired: Boolean,
      lastModifiedSeconds: Long
  ) =
    AssetResponse.Found(
      content,
      contentLength,
      HttpDate.fromEpochSecond(lastModifiedSeconds).toOption,
      toMediaType(fileName),
      isGzipped,
      isExpired
    )

  def noopAssetContent = fs2.Stream.empty

  def assetSegments(
      name: String,
      docs: Documentation
  ): Path[AssetPath] = {
    case p :+ s =>
      val i = s.lastIndexOf('-')
      val assetPath =
        if (i > 0) {
          val (resourceName, digest) = s.splitAt(i)
          AssetPath(p, Some(digest.drop(1)), resourceName)
        } else AssetPath(p, None, s)
      Some((Valid(assetPath), Nil))
    case Nil => None
  }

  private val gzipSupport: RequestHeaders[Boolean] =
    headers => Valid(headers.get(`Accept-Encoding`).exists(_.satisfiedBy(ContentCoding.gzip)))

  private val ifModifiedSince: RequestHeaders[Option[HttpDate]] =
    headers => Valid(headers.get(`If-Modified-Since`).map(_.date))

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

      Response(headers = headers, body = data)
  }

  def assetsEndpoint(
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

  private def toResourceUrl(
      pathPrefix: Option[String],
      assetRequest: AssetRequest
  ): Option[(URL, Boolean)] = {
    val assetPath = assetRequest.assetPath
    val path =
      if (assetPath.path.nonEmpty)
        assetPath.path.mkString("", "/", s"/${assetPath.name}")
      else assetPath.name
    lazy val resourcePath = pathPrefix.getOrElse("") ++ s"/$path"

    def nonGzippedResourceUrl = Option(getClass.getResource(resourcePath)).map((_, false))
    def gzippedResourceUrl = Option(getClass.getResource(s"$resourcePath.gz")).map((_, true))
    def resourceUrl =
      if (assetRequest.isGzipSupported) gzippedResourceUrl.orElse(nonGzippedResourceUrl)
      else nonGzippedResourceUrl

    def hasDigest(digest: String) = digests.get(path).contains(digest)

    assetPath.digest match {
      case None                                => resourceUrl
      case Some(digest) if (hasDigest(digest)) => resourceUrl
      case Some(digest)                        => None
    }
  }

  private def toMediaType(name: String): Option[MediaType] =
    name.lastIndexOf('.') match {
      case -1 => None
      case i  => MediaType.forExtension(name.substring(i + 1))
    }

  /**
    * @param pathPrefix Prefix to use to look up the resources in the classpath (like '/assets'). You
    *                   most probably never want to publish all your classpath resources.
    * @return A function that, given an [[AssetRequest]], builds an [[AssetResponse]] by
    *         looking for the requested asset in the classpath resources.
    */
  def assetsResources(
      blocker: Blocker,
      pathPrefix: Option[String] = None
  )(implicit cs: ContextShift[Effect]): AssetRequest => AssetResponse =
    assetRequest =>
      toResourceUrl(pathPrefix, assetRequest)
        .map {
          case (url, isGzipped) =>
            val urlConnection = url.openConnection

            val ifModifiedSinceSeconds = assetRequest.ifModifiedSince.map(_.epochSecond)
            val lastModifiedSeconds = urlConnection.getLastModified / 1000
            val isExpired =
              ifModifiedSinceSeconds.map(_ < lastModifiedSeconds).getOrElse(true)

            foundAssetResponse(
              content = readInputStream(Effect.delay(url.openStream), DefaultBufferSize, blocker),
              contentLength = urlConnection.getContentLengthLong,
              fileName = assetRequest.assetPath.name,
              isGzipped = isGzipped,
              isExpired = isExpired,
              lastModifiedSeconds = lastModifiedSeconds
            )
        }
        .getOrElse(notFoundAssetResponse)

}
