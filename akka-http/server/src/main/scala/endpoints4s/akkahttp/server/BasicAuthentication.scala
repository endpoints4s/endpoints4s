package endpoints4s.akkahttp.server

import akka.http.scaladsl.model.headers.{
  Authorization,
  BasicHttpCredentials,
  HttpChallenges,
  `WWW-Authenticate`
}
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, Uri, StatusCodes => AkkaStatusCodes}
import akka.http.scaladsl.server.{Directive1, Directives}
import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra.Documentation
import endpoints4s.{Invalid, Tupler, Valid, Validated, algebra}

/** @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with EndpointsWithCustomErrors {

  private[endpoints4s] def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Documentation
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, Credentials, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = {
    new Request[Out] {
      type UrlAndHeaders = (U, H, Credentials)

      lazy val authHeader: RequestHeaders[Option[Credentials]] =
        httpHeaders =>
          Valid(
            httpHeaders.header[Authorization].flatMap {
              case Authorization(BasicHttpCredentials(username, password)) =>
                Some(Credentials(username, password))
              case _ => None
            }
          )

      lazy val matchAndParseHeadersDirective: Directive1[Validated[UrlAndHeaders]] =
        Directives.extractRequest.map(authHeader.decode).flatMap {
          case Valid(None) =>
            Directives.complete(
              HttpResponse(
                AkkaStatusCodes.Unauthorized,
                collection.immutable.Seq[HttpHeader](
                  `WWW-Authenticate`(HttpChallenges.basic("Realm"))
                )
              )
            )
          case Valid(Some(credentials)) =>
            matchAndProvideParsedUrlAndHeadersData(method, url, headers)
              .map(_.map { case (u, h) => (u, h, credentials) })
          case invalid: Invalid => Directives.provide(invalid)
        }

      def parseEntityDirective(urlAndHeaders: UrlAndHeaders): Directive1[Out] =
        entity.map { e =>
          val (u, h, c) = urlAndHeaders
          tuplerUEHCred(tuplerUE(u, e), tuplerHCred(h, c))
        }

      def uri(out: Out): Uri = {
        val (ue, _) = tuplerUEHCred.unapply(out)
        val (u, _) = tuplerUE.unapply(ue)
        url.uri(u)
      }

    }
  }

}
