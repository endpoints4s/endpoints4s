package endpoints.scalaj.client

import java.util.Base64

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    (credentials) => {
      Seq(("Authorization", "Basic " + new String(Base64.getEncoder.encode((credentials.username + ":" + credentials.password).getBytes))))
    }

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](response: Response[A], docs: Documentation): Response[Option[A]] =
    resp =>
      if (resp.code == 403) _ => Right(None)
      else entity => response(resp)(entity).right.map(Some(_))

}
