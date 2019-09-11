package endpoints.akkahttp.client

import akka.http.scaladsl.model.headers._
import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication { self: Endpoints =>

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    (credentials, headers) => {
      headers :+ Authorization(BasicHttpCredentials(credentials.username, credentials.password))
    }

}
