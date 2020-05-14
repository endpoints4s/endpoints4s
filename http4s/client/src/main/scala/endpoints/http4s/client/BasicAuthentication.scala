package endpoints.http4s.client

import endpoints.{Tupler, algebra}
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
import org.http4s.headers.Authorization
import org.http4s.BasicCredentials

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication {
  self: EndpointsWithCustomErrors =>

  private[endpoints] def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Documentation
  )(
      implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, Credentials, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = {
    val basicAuthenticationHeader: RequestHeaders[Credentials] =
      (credentials, request) => {
        request.putHeaders(
          Authorization(
            BasicCredentials(credentials.username, credentials.password)
          )
        )
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
