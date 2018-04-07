package endpoints.algebra

import endpoints.Tupler
import endpoints.algebra.BasicAuthentication.Credentials

/**
  * Provides vocabulary to describe endpoints that use Basic HTTP authentication.
  *
  * This trait works fine, but developers are likely to implement their own
  * authentication mechanism, specific to their application.
  */
trait BasicAuthentication extends Endpoints {

  /**
    * Credentials encoded as HTTP Basic Auth header
    *
    * In routing interpreters if header is not present it should match the route and return 401 Unauthorized.
    * @return
    */
  private[endpoints] def basicAuthenticationHeader: RequestHeaders[Credentials]

  /**
    * @param response Inner response (in case the authentication succeeds)
    * @param documentation Description of the authentication error
    */
  private[endpoints] def authenticated[A](response: Response[A], documentation: String = ""): Response[Option[A]] // FIXME Use an extensible type to model authentication failure

  /**
    * Describes an endpoint protected by Basic HTTP authentication
    */
  def authenticatedEndpoint[A, B, C, D, AB, DCred](
    method: Method,
    url: Url[A],
    requestEntity: RequestEntity[B] = emptyRequest,
    requestHeaders: RequestHeaders[D] = emptyHeaders,
    response: Response[C],
    documentation: String = ""
  )(implicit
    tuplerAB: Tupler.Aux[A, B, AB],
    tuplerDCred: Tupler.Aux[D, Credentials, DCred],
    tuplerABC: Tupler[AB, DCred]
  ): Endpoint[tuplerABC.Out, Option[C]] =
    endpoint(request(method, url, requestEntity, requestHeaders ++ basicAuthenticationHeader), authenticated(response, documentation))

}

object BasicAuthentication {
  case class Credentials(username: String, password: String)
}
