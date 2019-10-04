package endpoints.sttp.client

import endpoints.{Tupler, algebra}
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

import scala.language.higherKinds

/**
  * @group interpreters
  */
trait BasicAuthentication[R[_]] extends algebra.BasicAuthentication { self: Endpoints[R] =>

  private[endpoints] def authenticatedRequest[U, E, H, UE, HCred, Out](
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
    (credentials, request) => {
      request.auth.basic(credentials.username, credentials.password)
    }
    request(method, url, entity, requestDocs, headers ++ basicAuthenticationHeader)
  }

}
