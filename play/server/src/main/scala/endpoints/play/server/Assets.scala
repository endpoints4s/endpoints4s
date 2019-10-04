package endpoints.play.server

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import endpoints.{Invalid, Valid, algebra}
import endpoints.algebra.Documentation
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc.Results
import play.mvc.Http.HeaderNames

/**
  * Interpreter for [[algebra.Assets]] that performs routing using Play framework
  *
  * @group interpreters
  */
trait Assets extends algebra.Assets with Endpoints {

  /**
    * @param assetPath Path of the requested asset
    * @param isGzipSupported Whether the client supports gzip encoding or not
    */
  case class AssetRequest(assetPath: AssetPath, isGzipSupported: Boolean)

  /**
    * {{{
    *   // foo/bar/baz-123abc
    *   AssetPath("foo" :: "bar" :: Nil, "123abc", "baz")
    * }}}
    */
  case class AssetPath(path: Seq[String], digest: String, name: String)

  /**
    * Convenient constructor for building an asset from its name, without
    * providing its digest.
    *
    * Useful for reverse routing:
    *
    * {{{
    *   myAssetsEndpoint.call(asset("baz"))
    * }}}
    *
    * @param name Asset name
    * @return An `AssetRequest` describing the asset. There is no path prefixing the
    *         name. Throws an exception if there is no digest for `name`.
    */
  def asset(name: String): AssetRequest = makeAsset(None, name)

  /**
    * Convenient constructor for building an asset from its name and
    * path, without providing its digest.
    *
    * Useful for reverse routing:
    *
    * {{{
    *   myAssetsEndpoint.call(asset("foo/bar", "baz"))
    * }}}
    *
    * @param path Asset path (no leading nor trailing slash)
    * @param name Asset name
    * @return An `AssetRequest` describing the asset. Throws an exception if
    *         there is no digest for `name`.
    */
  def asset(path: String, name: String): AssetRequest = makeAsset(Some(path), name)

  private def makeAsset(path: Option[String], name: String): AssetRequest = {
    val rawPath = path.fold(name)(p => s"$p/$name")
    val digest = digests.getOrElse(rawPath, throw new Exception(s"No digest for asset $rawPath"))
    val assetPath = AssetPath(path.fold(Seq.empty[String])(_.split("/")), digest, name)
    AssetRequest(assetPath, isGzipSupported = false) // HACK isGzipSupported makes no sense here
  }

  /**
    * An [[AssetResponse]] is either [[AssetNotFound]] (if the asset has not been found on
    * the server) or [[Found]] otherwise.
    */
  sealed trait AssetResponse
  case object AssetNotFound extends AssetResponse
  /**
    * @param data Asset content
    * @param contentLength Size, if known
    * @param contentType Content type, if known
    * @param isGzipped Whether `data` contains the gzipped version of the asset
    */
  case class Found(
    data: Source[ByteString, _],
    contentLength: Option[Long],
    contentType: Option[String],
    isGzipped: Boolean
  ) extends AssetResponse

  /**
    * Decodes and encodes an [[AssetPath]] into a URL path.
    */
  def assetSegments(name: String, docs: Documentation): Path[AssetPath] = {
    val stringPath = segment[String](name, docs)
    new Path[AssetPath] {
      def decode(segments: List[String]) =
        segments.reverse match {
          case s :: p =>
            val i = s.lastIndexOf('-')
            if (i > 0) {
              val (name, digest) = s.splitAt(i)
              Some((Valid(AssetPath(p.reverse, digest.drop(1), name)), Nil))
            } else Some((Invalid("Invalid asset segments"), Nil))
          case Nil => None
        }
      def encode(s: AssetPath) =
        s.path.foldRight(stringPath.encode(s"${s.name}-${s.digest}"))((segment, path) => s"${stringPath.encode(segment)}/$path")
    }
  }

  private lazy val gzipSupport: RequestHeaders[Boolean] =
    headers => Valid(headers.get(HeaderNames.ACCEPT_ENCODING).exists(_.contains("gzip")))

  /**
    * An endpoint for serving assets.
    *
    * @param url URL description
    * @return An HTTP endpoint serving assets
    */
  def assetsEndpoint(url: Url[AssetPath], docs: Documentation, notFoundDocs: Documentation): Endpoint[AssetRequest, AssetResponse] = {
    val req =
      invariantFunctorRequest.inmap( // TODO remove this boilerplate using play-products
        request(Get, url, headers = gzipSupport),
        (t: (AssetPath, Boolean)) => AssetRequest(t._1, t._2),
        (assetRequest: AssetRequest) => (assetRequest.assetPath, assetRequest.isGzipSupported)
      )

    endpoint(req, assetResponse)
  }

  /**
    * Builds a Play `Result` from an [[AssetResponse]]
    */
  private def assetResponse: Response[AssetResponse] = {
      case Found(resource, maybeLength, maybeContentType, isGzipped) =>
        val result =
          Results.Ok
            .sendEntity(HttpEntity.Streamed(resource, maybeLength, maybeContentType))
            .withHeaders(
              HeaderNames.CONTENT_DISPOSITION -> "inline",
              HeaderNames.CACHE_CONTROL -> "public, max-age=31536000"
            )
        if (isGzipped) result.withHeaders(HeaderNames.CONTENT_ENCODING -> "gzip") else result
      case AssetNotFound => NotFound
    }

  /**
    * @param pathPrefix Prefix to use to look up the resources in the classpath. You
    *                   most probably never want to publish all your classpath resources.
    * @return A function that, given an [[AssetRequest]], builds an [[AssetResponse]] by
    *         looking for the requested asset in the classpath resources.
    */
  def assetsResources(pathPrefix: Option[String] = None): AssetRequest => AssetResponse =
    assetRequest => {
      val assetInfo = assetRequest.assetPath
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
            Found(
              StreamConverters.fromInputStream(() => stream),
              Some(stream.available().toLong),
              playComponents.fileMimeTypes.forFileName(assetInfo.name).orElse(Some(ContentTypes.BINARY)),
              isGzipped
            )
          }.getOrElse(AssetNotFound)
      } else AssetNotFound
    }

}
