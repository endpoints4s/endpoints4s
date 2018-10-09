package endpoints.play.server

import java.util.Base64

import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra
import endpoints.algebra.Documentation
import play.api.http.HeaderNames
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.mvc.Results

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Extracts the credentials from the request headers.
    * In case of absence of credentials, returns an `Unauthorized` result.
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    headers =>
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
        .toRight(Results.Unauthorized.withHeaders(HeaderNames.WWW_AUTHENTICATE -> "Basic realm=\"Some custom name\"")) // TODO Make the realm extensible

  /**
    * Authorization failures can be signaled by returning `None` in the endpoint implementation.
    * In such a case, a `Forbidden` result is returned.
    */
  private[endpoints] def authenticated[A](response: Response[A], docs: Documentation): Response[Option[A]] = {
    case Some(a) => response(a)
    case None => Results.Forbidden
  }

}
