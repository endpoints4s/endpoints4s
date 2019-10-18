package endpoints.scalaj.client

import java.util.Base64

import endpoints.{Tupler, algebra}
import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

/**
  * @group interpreters
  */
trait BasicAuthentication extends algebra.BasicAuthentication with EndpointsWithCustomErrors {

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
      (credentials) => {
        Seq(("Authorization", "Basic " + new String(Base64.getEncoder.encode((credentials.username + ":" + credentials.password).getBytes))))
      }

    request(method, url, entity, requestDocs, headers ++ basicAuthenticationHeader)
  }

}
