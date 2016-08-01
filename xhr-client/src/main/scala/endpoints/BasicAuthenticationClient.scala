package endpoints

import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.window.btoa

trait BasicAuthenticationClient extends BasicAuthenticationAlg with XhrClient {

  def authenticatedGet[A](url: Url[A])(implicit tupler: Tupler[A, Credentials]): Request[tupler.Out] =
    out => {
      val (a, credentials) = tupler.unapply(out)
      val xhr = new XMLHttpRequest
      xhr.open("GET", url.encodeUrl(a))
      xhr.setRequestHeader("Authorization", "Basic " + btoa(credentials.username + ":" + credentials.password))
      (xhr, None)
    }

}
