package endpoints4s.algebra

import endpoints4s.Tupler
import endpoints4s.algebra.BasicAuthentication.Credentials

/** Provides vocabulary to describe endpoints that use Basic HTTP authentication.
  *
  * This trait works fine, but developers are likely to implement their own
  * authentication mechanism, specific to their application.
  *
  * @group algebras
  */
trait BasicAuthentication extends EndpointsWithCustomErrors {

  /** A response that can either be Forbidden (403) or the given `Response[A]`.
    *
    * The returned `Response[Option[A]]` signals “forbidden” with a `None` value.
    *
    * @param responseA Inner response (in case the authentication succeeds)
    * @param docs Description of the authentication error
    */
  private[endpoints4s] final def authenticated[A](
      responseA: Response[A],
      docs: Documentation = None
  ): Response[
    Option[A]
  ] = // FIXME Use an extensible type to model authentication failure
    responseA
      .orElse(response(Forbidden, emptyResponse, docs))
      .xmap(_.fold[Option[A]](Some(_), _ => None))(_.toLeft(()))

  /** A request with the given `method`, `url`, `entity` and `headers`, but
    * which also contains the Basic Authentication credentials in its
    * “Authorization” header.
    *
    * The `Out` type aggregates together the URL information `U`, the entity
    * information `E`, the headers information `H`, and the `Credentials`.
    *
    * In case the authentication credentials are missing from the request,
    * servers reject the request with an Unauthorized (401) status code.
    */
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
  ): Request[Out]

  /** Describes an endpoint protected by Basic HTTP authentication
    * @group operations
    */
  def authenticatedEndpoint[U, E, R, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      response: Response[R],
      requestEntity: RequestEntity[E] = emptyRequest,
      requestHeaders: RequestHeaders[H] = emptyRequestHeaders,
      unauthenticatedDocs: Documentation = None,
      requestDocs: Documentation = None,
      endpointDocs: EndpointDocs = EndpointDocs()
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, Credentials, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Endpoint[Out, Option[R]] =
    endpoint(
      authenticatedRequest(
        method,
        url,
        requestEntity,
        requestHeaders,
        requestDocs
      ),
      authenticated(response, unauthenticatedDocs),
      endpointDocs
    )

}

object BasicAuthentication {
  case class Credentials(username: String, password: String)
}
