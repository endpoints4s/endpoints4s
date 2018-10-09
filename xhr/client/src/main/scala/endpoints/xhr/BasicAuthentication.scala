package endpoints.xhr

import endpoints.algebra
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
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

  /**
    * Checks that the result is not `Forbidden`
    */
  private[endpoints] def authenticated[A](response: Response[A], notFoundDocs: Documentation): Response[Option[A]] =
    xhr =>
      // We don’t care of 401 because we always set the Authorization header. We only care about 403.
      if (xhr.status == 403) Right(None) // We use `Right` to make handling of authentication failures explicit
      else response(xhr).right.map(Some(_))

}
