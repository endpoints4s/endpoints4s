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
  //TODO we could implement this in algebra via header("Authorization).xmap() but how to enforce 401?
  private[endpoints] def basicAuthenticationHeader: RequestHeaders[Credentials]

  /**
    * @param response Inner response (in case the authentication succeeds)
    * @param documentation Description of the authentication error
    */
  private[endpoints] def authenticated[A](response: Response[A], documentation: String = ""): Response[Option[A]] // FIXME Use an extensible type to model authentication failure

  /**
    * Describes an endpoint protected by Basic HTTP authentication
    */
  def authenticatedEndpoint[U, E, R, H, UE, DCred](
    method: Method,
    url: Url[U],
    response: Response[R],
    requestEntity: RequestEntity[E] = emptyRequest,
    requestHeaders: RequestHeaders[H] = emptyHeaders,
    unauthenticatedDoc: String = "",
    summary: Option[String] = None,
    description: Option[String] = None
  )(implicit
    tuplerAB: Tupler.Aux[U, E, UE],
    tuplerDCred: Tupler.Aux[H, Credentials, DCred],
    tuplerABDCred: Tupler[UE, DCred]
  ): Endpoint[tuplerABDCred.Out, Option[R]] =
    endpoint(
      request(method, url, requestEntity, requestHeaders ++ basicAuthenticationHeader),
      authenticated(response, unauthenticatedDoc),
      summary,
      description
    )

}

object BasicAuthentication {
  case class Credentials(username: String, password: String)
}
