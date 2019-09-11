package endpoints.sttp.client

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

import scala.language.higherKinds

/**
  * @group interpreters
  */
trait BasicAuthentication[R[_]] extends algebra.BasicAuthentication { self: Endpoints[R] =>

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    (credentials, request) => {
      request.auth.basic(credentials.username, credentials.password)
    }

}
