package endpoints4s.pekkohttp.server

import org.apache.pekko.http.scaladsl.model.headers.{
  Authorization,
  BasicHttpCredentials,
  HttpChallenges,
  `WWW-Authenticate`
}
import org.apache.pekko.http.scaladsl.model.{
  HttpHeader,
  HttpResponse,
  Uri,
  StatusCodes => PekkoStatusCodes
}
import org.apache.pekko.http.scaladsl.server.{Directive1, Directives}
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

      lazy val unauthorized: Directive1[Validated[UrlAndHeaders]] = Directives.complete(
        HttpResponse(
          PekkoStatusCodes.Unauthorized,
          collection.immutable.Seq[HttpHeader](
            `WWW-Authenticate`(HttpChallenges.basic("Realm"))
          )
        )
      )

      lazy val matchAndParseHeadersDirective: Directive1[Validated[UrlAndHeaders]] = for {
        uh <- matchAndProvideParsedUrlAndHeadersData(method, url, headers)
        credentials <- Directives.extractRequest.map(authHeader.decode)
        result <- credentials match {
          case Valid(Some(credentials)) =>
            Directives.provide {
              uh.map { case (u, h) =>
                (u, h, credentials)
              }
            }
          case Valid(None)      => unauthorized
          case invalid: Invalid => Directives.provide[Validated[UrlAndHeaders]](invalid)
        }
      } yield result

      def parseEntityDirective(urlAndHeaders: UrlAndHeaders): Directive1[Out] =
        entity.map { entityData =>
          val (urlData, headersData, credentials) = urlAndHeaders
          tuplerUEHCred(tuplerUE(urlData, entityData), tuplerHCred(headersData, credentials))
        }

      def uri(out: Out): Uri = {
        val (urlAndEntityData, _) = tuplerUEHCred.unapply(out)
        val (urlData, _) = tuplerUE.unapply(urlAndEntityData)
        url.uri(urlData)
      }

    }
  }

}
