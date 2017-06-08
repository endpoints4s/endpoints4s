package endpoints.scalaj.client

import java.util.Base64

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthentication: RequestHeaders[Credentials] =
    (credentials) => {
      Seq(("Authorization", "Basic " + new String(Base64.getEncoder.encode((credentials.username + ":" + credentials.password).getBytes))))
    }

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](response: Response[A]): Response[Option[A]] =
    resp =>
      if (resp.code == 403) Right(None)
      else response(resp).right.map(Some(_))

}
