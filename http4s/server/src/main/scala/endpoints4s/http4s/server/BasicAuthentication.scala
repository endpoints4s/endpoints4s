package endpoints4s.http4s.server

import cats.data.NonEmptyList
import cats.implicits._
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
    new Request[Out] {

      type UrlAndHeaders = (U, H, Credentials)

      def matchAndParseHeaders(
          http4sRequest: Http4sRequest
      ): Option[Either[Http4sResponse, Validated[UrlAndHeaders]]] =
        matchAndParseHeadersAsRight(method, url, headers, http4sRequest)
          .map { errorResponseOrValidatedUrlAndHeaders =>
            basicAuthenticationHeader(http4sRequest.headers) match {
              case Valid(Some(credentials)) =>
                errorResponseOrValidatedUrlAndHeaders
                  .map(_.map { case (u, h) => (u, h, credentials) })
              case _ => Left(unauthorizedRequestResponse)
            }
          }

      def parseEntity(
          urlAndHeaders: UrlAndHeaders,
          http4sRequest: Http4sRequest
      ): Effect[Either[Http4sResponse, Out]] =
        entity(http4sRequest).map(_.map { entityData =>
          tuplerUEHC(
            tuplerUE(urlAndHeaders._1, entityData),
            tuplerHC(urlAndHeaders._2, urlAndHeaders._3)
          )
        })

    }
  }

}
