package endpoints
package openapi

import endpoints.algebra.BasicAuthentication.Credentials

/**
  * Interpreter for [[algebra.BasicAuthentication]] that produces
  * OpenAPI documentation.
  */
trait BasicAuthentication
  extends algebra.BasicAuthentication
    with Endpoints {

  private[endpoints] def basicAuthentication: RequestHeaders[Credentials] =
    DocumentedHeaders(DocumentedHeader("Authorization", None, required = true) :: Nil)

  private[endpoints] def authenticated[A](response: Response[A], documentation: String): Response[Option[A]] =
    DocumentedResponse(401, documentation, content = Map.empty) :: response

}
