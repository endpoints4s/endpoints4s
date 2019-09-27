package endpoints
package openapi

import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
import endpoints.openapi.model.{SecurityRequirement, SecurityScheme}

/**
  * Interpreter for [[algebra.BasicAuthentication]] that produces
  * OpenAPI documentation.
  *
  * @group interpreters
  */
trait BasicAuthentication
  extends algebra.BasicAuthentication
    with Endpoints
    with StatusCodes {

  def basicAuthenticationSchemeName: String = "HttpBasic"

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
  ): Request[Out] =
    request(method, url, entity, requestDocs, headers) // Documentation about authentication is done below by overriding authenticatedEndpoint

  override def authenticatedEndpoint[U, E, R, H, UE, HCred, Out](
    method: Method,
    url: Url[U],
    response: Response[R],
    requestEntity: RequestEntity[E] = emptyRequest,
    requestHeaders: RequestHeaders[H] = emptyHeaders,
    unauthenticatedDocs: Documentation = None,
    requestDocs: Documentation = None,
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  )(implicit
    tuplerUE: Tupler.Aux[U, E, UE],
    tuplerHCred: Tupler.Aux[H, Credentials, HCred],
    tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Endpoint[Out, Option[R]] =
    super.authenticatedEndpoint(method, url, response, requestEntity, requestHeaders, unauthenticatedDocs, requestDocs, summary, description, tags)
      .withSecurity(SecurityRequirement(basicAuthenticationSchemeName, SecurityScheme.httpBasic))
}
