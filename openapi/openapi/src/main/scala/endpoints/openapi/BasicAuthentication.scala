package endpoints
package openapi

import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation
import endpoints.openapi.model.{OperationSecurity, SecurityScheme}

/**
  * Interpreter for [[algebra.BasicAuthentication]] that produces
  * OpenAPI documentation.
  *
  * @group interpreters
  */
trait BasicAuthentication
  extends algebra.BasicAuthentication
    with Endpoints {

  private[endpoints] def basicAuthenticationHeader: RequestHeaders[Credentials] =
    DocumentedHeaders(Nil) // supported by OAS3 security schemes

  private[endpoints] def authenticated[A](response: Response[A], docs: Documentation): Response[Option[A]] =
    DocumentedResponse(401, docs.getOrElse(""), content = Map.empty) :: response

  /**
    * Describes an endpoint protected by Basic HTTP authentication
    */
  override def authenticatedEndpoint[U, E, R, H, UE, HCred, Out](
    method: Method,
    url: Url[U],
    response: Response[R],
    requestEntity: RequestEntity[E] = emptyRequest,
    requestHeaders: RequestHeaders[H] = emptyHeaders,
    unauthenticatedDocs: Documentation = None,
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  )(implicit
    tuplerUE: Tupler.Aux[U, E, UE],
    tuplerHCred: Tupler.Aux[H, Credentials, HCred],
    tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Endpoint[Out, Option[R]] =
    super.authenticatedEndpoint(method, url, response, requestEntity, requestHeaders, unauthenticatedDocs, summary, description, tags)
      .withSecurity(OperationSecurity("HttpBasic", SecurityScheme.httpBasic))

}
