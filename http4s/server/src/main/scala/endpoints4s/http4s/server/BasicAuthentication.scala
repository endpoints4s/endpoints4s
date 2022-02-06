package endpoints4s.http4s.server

import cats.data.NonEmptyList
import endpoints4s.{Tupler, Valid, Validated}
import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra.Documentation
import org.http4s
import org.http4s.headers.{Authorization, `WWW-Authenticate`}
import org.http4s.{BasicCredentials, Challenge}

/** @group interpreters
  */
trait BasicAuthentication
    extends EndpointsWithCustomErrors
    with endpoints4s.algebra.BasicAuthentication {

  private val unauthorizedRequestResponse = http4s
    .Response[Effect](Unauthorized)
    .withHeaders(
      `WWW-Authenticate`(
        NonEmptyList.of(Challenge("Basic", "Realm", Map("charset" -> "UTF-8")))
      )
    )

  private[endpoints4s] def basicAuthenticationHeader: RequestHeaders[Option[Credentials]] =
    headers =>
      Valid(
        headers
          .get[Authorization]
          .flatMap { authHeader =>
            authHeader.credentials match {
              case BasicCredentials(username, password) =>
                Some(Credentials(username, password))
              case _ => None
            }
          }
      )

  def authenticatedRequest[U, E, H, UE, HC, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Documentation = None
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHC: Tupler.Aux[H, Credentials, HC],
      tuplerUEHC: Tupler.Aux[UE, HC, Out]
  ): Request[Out] = {
    val methodArg = method
    val urlArg = url
    val entityArg = entity
    val headersArg = headers
    new Request[Out] {
      type UrlData = U
      type HeadersData = (H, Option[Credentials])
      type EntityData = E
      def method: Method = methodArg
      def url: Url[UrlData] = urlArg
      def headers: RequestHeaders[HeadersData] = headersArg ++ basicAuthenticationHeader
      def entity: RequestEntity[EntityData] = entityArg
      def aggregateAndValidate(
          urlData: UrlData,
          headersData: HeadersData,
          entityData: EntityData
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
            Valid(tuplerUEHC(tuplerUE(urlData, entityData), tuplerHC(h, credentials)))
        }
      def matchAndParseHeaders(
          http4sRequest: Http4sRequest
      ): Option[Either[Http4sResponse, Validated[(U, (H, Option[Credentials]))]]] = {
        matchAndParseHeadersAsRight(method, url, headers, http4sRequest).map(_.flatMap {
          case Valid((_, (_, None /* credentials */ ))) => Left(unauthorizedRequestResponse)
          case validatedUrlAndHeaders                   => Right(validatedUrlAndHeaders)
        })
      }
    }
  }

}
