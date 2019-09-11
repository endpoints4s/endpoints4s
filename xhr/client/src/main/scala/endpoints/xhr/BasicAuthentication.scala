package endpoints.xhr

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import org.scalajs.dom.window.btoa

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] =
    (credentials, xhr) => {
      xhr.setRequestHeader("Authorization", "Basic " + btoa(credentials.username + ":" + credentials.password))
      ()
    }

}
