package endpoints4s.akkahttp.server

import akka.http.scaladsl.model.headers.{
  Authorization,
  BasicHttpCredentials,
  HttpChallenges,
  `WWW-Authenticate`
}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCodes => AkkaStatusCodes}
import akka.http.scaladsl.server.{Directive1, Directives}
import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra.Documentation
import endpoints4s.{Tupler, Valid, Validated, algebra}

/** @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with EndpointsWithCustomErrors {

  private[endpoints4s] def authenticatedRequest[U, E, H, UE, HCred, Out](
      m: Method,
      u: Url[U],
      entity: RequestEntity[E],
      h: RequestHeaders[H],
      requestDocs: Documentation
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, Credentials, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] =
    new Request[Out] {
      type UrlData = U
      def url: Url[UrlData] = u
      def method: Method = m
      type RequestEntityData = E
      type HeaderData = (H, Option[Credentials])
      def requestEntity: RequestEntity[RequestEntityData] = entity
      def headers: RequestHeaders[HeaderData] = h ++ authHeader
      def documentation: Documentation = requestDocs

      ///
      private[server] def aggregateAndValidate(
          urlData: UrlData,
          entityData: RequestEntityData,
          headersData: HeaderData
      ): Validated[Out] =
        headersData match {
          case (_, None) =>
            // Note: in practice that should not happen because the method `aggregateAndValidate` is
            // only called from the final method `matches`, if `matchAndParseHeaders` succeeded.
            // However, here we override `matchAndParseHeaders` to fail in case the credentials are missing.
            sys.error(
              "This request transformation is currently unsupported. You can't transform further an authenticated request."
            )
          case (h, Some(credentials)) =>
            Valid(tuplerUEHCred(tuplerUE(urlData, entityData), tuplerHCred(h, credentials)))
        }
      ///

      lazy val authHeader: RequestHeaders[Option[Credentials]] =
        httpHeaders =>
          Valid(
            httpHeaders.header[Authorization].flatMap {
              case Authorization(BasicHttpCredentials(username, password)) =>
                Some(Credentials(username, password))
              case _ => None
            }
          )

      private[server] def matchAndParseHeadersDirective
          : Directive1[Validated[(UrlData, HeaderData)]] =
        matchAndProvideParsedUrlAndHeadersData(method, url, headers).flatMap {
          case Valid((_, (_, None /* credentials */ ))) =>
            Directives.complete(
              HttpResponse(
                AkkaStatusCodes.Unauthorized,
                collection.immutable.Seq[HttpHeader](
                  `WWW-Authenticate`(HttpChallenges.basic("Realm"))
                )
              )
            )
          case validatedUrlAndHeaders => Directives.provide(validatedUrlAndHeaders)
        }

      def urlData(out: Out): UrlData = {
        val (ue, _) = tuplerUEHCred.unapply(out)
        val (u, _) = tuplerUE.unapply(ue)
        u
      }
    }

}
