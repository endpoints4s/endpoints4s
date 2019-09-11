package endpoints.scalaj.client

import java.util.Base64

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

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

}
