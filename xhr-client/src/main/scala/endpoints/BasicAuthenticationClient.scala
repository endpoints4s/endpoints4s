package endpoints

import org.scalajs.dom.window.btoa

trait BasicAuthenticationClient extends BasicAuthenticationAlg with XhrClient {

  lazy val basicAuthentication: Headers[Credentials] =
    (credentials, xhr) => {
      xhr.setRequestHeader("Authorization", "Basic " + btoa(credentials.username + ":" + credentials.password))
      ()
    }

}
