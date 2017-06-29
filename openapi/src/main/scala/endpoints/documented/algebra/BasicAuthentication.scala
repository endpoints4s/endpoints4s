package endpoints
package documented
package algebra

import endpoints.algebra.BasicAuthentication.Credentials

/**
  * Algebra interface for describing HTTP Basic authentication.
  *
  * This interface is modeled after [[endpoints.algebra.BasicAuthentication]].
  */
trait BasicAuthentication extends Endpoints {

  /**
    * Credentials encoded as HTTP Basic Auth header
    *
    * In routing interpreters if header is not present it should match the route and return 401 Unauthorized.
    * @return
    */
  private[endpoints] def basicAuthentication: RequestHeaders[Credentials]

  /**
    * @param response Inner response (in case the authentication succeeds)
    * @param description Description of the authentication error
    */
  private[endpoints] def authenticated[A](response: Response[A], description: String): Response[Option[A]]

  /**
    * Describes an endpoint protected by Basic HTTP authentication
    */
  def authenticatedEndpoint[A, B, C, AB](
    method: Method,
    url: Url[A],
    requestEntity: RequestEntity[B] = emptyRequest,
    response: Response[C],
    description: String
  )(implicit
    tuplerAB: Tupler.Aux[A, B, AB],
    tuplerABC: Tupler[AB, Credentials]
  ): Endpoint[tuplerABC.Out, Option[C]] =
    endpoint(request(method, url, requestEntity, basicAuthentication), authenticated(response, description))

}
