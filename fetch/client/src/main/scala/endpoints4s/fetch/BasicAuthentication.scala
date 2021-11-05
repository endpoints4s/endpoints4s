package endpoints4s.fetch

import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra.Documentation
import endpoints4s.Tupler
import endpoints4s.algebra
import org.scalajs.dom.window.btoa

/** @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with EndpointsWithCustomErrors {

  private[endpoints4s] def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Documentation
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, Credentials, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = {
    val basicAuthenticationHeader: RequestHeaders[Credentials] = { (credentials, requestInit) =>
      requestInit.setRequestHeader(
        "Authorization",
        "Basic " + btoa(credentials.username + ":" + credentials.password)
      )
      ()
    }
    request(
      method,
      url,
      entity,
      requestDocs,
      headers ++ basicAuthenticationHeader
    )
  }

}
