package endpoints4s.akkahttp.client

import akka.http.scaladsl.model.headers._
import endpoints4s.{Tupler, algebra}
import endpoints4s.algebra.BasicAuthentication.Credentials
import endpoints4s.algebra.Documentation

/** @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication {
  self: EndpointsWithCustomErrors =>

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
    val basicAuthenticationHeader: RequestHeaders[Credentials] =
      (credentials, headers) => {
        headers :+ Authorization(
          BasicHttpCredentials(credentials.username, credentials.password)
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
