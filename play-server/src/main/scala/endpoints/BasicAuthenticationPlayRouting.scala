package endpoints

import java.util.Base64

import play.api.http.HeaderNames.AUTHORIZATION

trait BasicAuthenticationPlayRouting extends BasicAuthenticationAlg with EndpointPlayRouting {

  lazy val basicAuthentication: Headers[Credentials] =
    requestHeader =>
      requestHeader.headers.get(AUTHORIZATION)
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
        } // TODO Return Unauthorized instead of None

}
