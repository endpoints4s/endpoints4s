package endpoints.play.server

import java.util.Base64

import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
import endpoints.{Tupler, Valid, algebra}
import play.api.http.HeaderNames
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.streams.Accumulator
import play.api.mvc.{BodyParser, Results}

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  import playComponents.executionContext

  /**
    * Extracts the credentials from the request headers.
    * In case of absence of credentials, returns an `Unauthorized` result.
    */
  private lazy val basicAuthenticationHeader: RequestHeaders[Option[Credentials]] =
    headers =>
      Valid(
        headers.get(AUTHORIZATION)
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
    extractMethodUrlAndHeaders(method, url, headers ++ basicAuthenticationHeader)
      .toRequest[Out] {
        case (_, (_, None)) =>
          BodyParser(_ => Accumulator.done(Left(Results.Unauthorized.withHeaders(HeaderNames.WWW_AUTHENTICATE -> "Basic realm=Realm"))))
        case (u, (h, Some(credentials))) =>
          entity.map(e => tuplerUEHC(tuplerUE(u, e), tuplerHC(h, credentials)))
      } { out =>
        val (ue, hc) = tuplerUEHC.unapply(out)
        val (u, _) = tuplerUE.unapply(ue)
        val (h, c) = tuplerHC.unapply(hc)
        (u, (h, Some(c)))
      }
  }

}
