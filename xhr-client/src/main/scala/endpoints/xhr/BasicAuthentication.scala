package endpoints.xhr

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import org.scalajs.dom.window.btoa

trait BasicAuthentication extends algebra.BasicAuthentication with Endpoints {

  /**
    * Supplies the credential into the request headers
    */
  private[endpoints] lazy val basicAuthentication: RequestHeaders[Credentials] =
    (credentials, xhr) => {
      xhr.setRequestHeader("Authorization", "Basic " + btoa(credentials.username + ":" + credentials.password))
      ()
    }

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](response: Response[A]): Response[Option[A]] =
    xhr =>
      // We don’t care of 401 because we always set the Authorization header. We only care about 403.
      if (xhr.status == 403) Right(None) // We use `Right` to make handling of authentication failures explicit
      else response(xhr).right.map(Some(_))

}
