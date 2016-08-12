package endpoints

import org.scalajs.dom.window.btoa

trait BasicAuthenticationXhrClient extends BasicAuthenticationAlg with EndpointXhrClient {

  lazy val basicAuthentication: Headers[Credentials] =
    (credentials, xhr) => {
      xhr.setRequestHeader("Authorization", "Basic " + btoa(credentials.username + ":" + credentials.password))
      ()
    }

}
