package endpoints

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import play.api.http.{ContentTypes, HttpEntity}
import play.api.libs.MimeTypes
import play.api.mvc.Results
import play.mvc.Http.HeaderNames

trait AssetPlayRouting extends AssetAlg with EndpointPlayRouting {

  case class AssetRequest(assetInfo: AssetPath, isGzipSupported: Boolean)

  case class AssetPath(path: Seq[String], digest: String, name: String)

  def asset(name: String): AssetRequest = makeAsset(None, name)

  def asset(path: String, name: String): AssetRequest = makeAsset(Some(path), name)

  private def makeAsset(path: Option[String], name: String): AssetRequest = {
    val rawPath = path.fold(name)(p => s"$p/$name")
    val digest = digests.getOrElse(rawPath, throw new Exception(s"No digest for asset $rawPath"))
    val assetPath = AssetPath(path.fold(Seq.empty[String])(_.split("/")), digest, name)
    AssetRequest(assetPath, isGzipSupported = false) // HACK isGzipSupported makes no sense here
  }

  // (data, content-length, content-type, gzipped)
  type AssetResponse = Option[(Source[ByteString, _], Option[Long], Option[String], Boolean)]

  lazy val assetSegments: Path[AssetPath] = {
    val stringPath = segment[String]
    new Path[AssetPath] {
      def decode(segments: List[String]) =
        segments.reverse match {
          case s :: p =>
            val i = s.lastIndexOf('-')
            if (i > 0) {
              val (name, digest) = s.splitAt(i)
              Some((AssetPath(p.reverse, digest.drop(1), name), Nil))
            } else None
          case Nil => None
        }
      def encode(s: AssetPath) =
        s.path.foldRight(stringPath.encode(s"${s.name}-${s.digest}"))((segment, path) => s"${stringPath.encode(segment)}/$path")
    }
  }

  private lazy val gzipSupport: Headers[Boolean] =
    request => request.headers.get(HeaderNames.ACCEPT_ENCODING).map(_.contains("gzip"))

  def assetsEndpoint(url: Url[AssetPath]): Endpoint[AssetRequest, AssetResponse] = {
    val request =
      invariantFunctorRequest.inmap( // TODO remove this boilerplate using play-products
        get(url, gzipSupport),
        (t: (AssetPath, Boolean)) => AssetRequest(t._1, t._2),
        (assetRequest: AssetRequest) => (assetRequest.assetInfo, assetRequest.isGzipSupported)
      )

    endpoint(request, assetResponse)
  }

  private def assetResponse: Response[AssetResponse] = {
      case Some((resource, maybeLength, maybeContentType, isGzipped)) =>
        val result =
          Results.Ok
            .sendEntity(HttpEntity.Streamed(resource, maybeLength, maybeContentType))
            .withHeaders(
              HeaderNames.CONTENT_DISPOSITION -> "inline",
              HeaderNames.CACHE_CONTROL -> "public, max-age=31536000"
            )
        if (isGzipped) result.withHeaders(HeaderNames.CONTENT_ENCODING -> "gzip") else result
      case None => Results.NotFound
    }

  def assetsResources(pathPrefix: Option[String] = None): AssetRequest => AssetResponse =
    assetRequest => {
      val assetInfo = assetRequest.assetInfo
      val path =
        if (assetInfo.path.nonEmpty) assetInfo.path.mkString("", "/", s"/${assetInfo.name}") else assetInfo.name
      if (digests.get(path).contains(assetInfo.digest)) {
        val resourcePath = pathPrefix.getOrElse("") ++ s"/$path"
        val maybeAsset = {
          def nonGzippedAsset = Option(getClass.getResourceAsStream(resourcePath)).map((_, false))
            if (assetRequest.isGzipSupported) {
              Option(getClass.getResourceAsStream(s"$resourcePath.gz")).map((_, true))
                .orElse(nonGzippedAsset)
            } else {
              nonGzippedAsset
            }
        }
        maybeAsset
          .map { case (stream, isGzipped) =>
            (
              StreamConverters.fromInputStream(() => stream),
              Some(stream.available().toLong),
              MimeTypes.forFileName(assetInfo.name).orElse(Some(ContentTypes.BINARY)),
              isGzipped
            )
          }
      } else None
    }

}
