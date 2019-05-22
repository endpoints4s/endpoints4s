package endpoints.play.client

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
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

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](inner: Response[A], docs: Documentation): Response[Option[A]] =
    (status, headers) =>
      if (status == 403) _ => Right(None)
      else wsResp => inner(status, headers)(wsResp).right.map(Some(_))

}
