package endpoints4s.play.server

import java.util.Base64
import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra.Documentation
import endpoints4s.{Tupler, Valid, Validated, algebra}
import play.api.http.HeaderNames
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.mvc.{RequestHeader, Results}

/** @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with EndpointsWithCustomErrors {

  /** Extracts the credentials from the request headers.
    * In case of absence of credentials, returns an `Unauthorized` result.
    */
  private lazy val basicAuthenticationHeader: RequestHeaders[Option[Credentials]] =
    headers =>
      Valid(
        headers
          .get(AUTHORIZATION)
          .filter(h => h.startsWith("Basic ")) // FIXME case sensitivity?
          .flatMap { h =>
            val userPassword =
              new String(Base64.getDecoder.decode(h.drop(6)))
            val i = userPassword.indexOf(':')
            if (i < 0) None
            else {
              val (user, password) = userPassword.splitAt(i)
              Some(Credentials(user, password.drop(1)))
            }
          }
      )

  def authenticatedRequest[U, E, H, UE, HC, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Documentation
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHC: Tupler.Aux[H, Credentials, HC],
      tuplerUEHC: Tupler.Aux[UE, HC, Out]
  ): Request[Out] = {
    val u = url
    val h = headers
    val m = method
    val e = entity
    new Request[Out] {
      type UrlData = U
      type EntityData = E
      type HeadersData = (H, Option[Credentials])

      def url: Url[UrlData] = u
      def headers: RequestHeaders[HeadersData] = h ++ basicAuthenticationHeader
      def method: Method = m
      def entity: RequestEntity[E] = e
      def aggregateAndValidate(
          urlData: UrlData,
          headersData: HeadersData,
          entityData: EntityData
      ): Validated[Out] =
        headersData match {
          case (_, None) =>
            sys.error(
              "This request transformation is currently unsupported. You can't transform further an authenticated request."
            )
          case (h, Some(credentials)) =>
            Valid(tuplerUEHC(tuplerUE(urlData, entityData), tuplerHC(h, credentials)))
        }
      def matchRequest(requestHeader: RequestHeader): Option[RequestEntity[Out]] = {
        matchRequestAndParseHeaders(requestHeader) {
          case (_, (_, None /* credentials */ )) =>
            requestEntityOf(
              Left(
                Results.Unauthorized
                  .withHeaders(HeaderNames.WWW_AUTHENTICATE -> "Basic realm=Realm")
              )
            )
          case (urlData, (headersData, Some(credentials))) =>
            requestEntityMap(entity) { entityData =>
              tuplerUEHC(tuplerUE(urlData, entityData), tuplerHC(headersData, credentials))
            }
        }
      }
      def urlData(a: Out): U = {
        val (ue, hc) = tuplerUEHC.unapply(a)
        val (u, _) = tuplerUE.unapply(ue)
        u
      }
    }
  }

}
