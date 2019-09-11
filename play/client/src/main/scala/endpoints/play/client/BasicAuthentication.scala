package endpoints.play.client

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import play.api.libs.ws.WSAuthScheme

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication { self: Endpoints =>

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    (credentials, request) => {
      request.withAuth(credentials.username, credentials.password, WSAuthScheme.BASIC)
    }

}
