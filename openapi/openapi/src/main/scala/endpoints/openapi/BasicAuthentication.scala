package endpoints
package openapi

import endpoints.algebra.BasicAuthentication.Credentials
import endpoints.algebra.Documentation

/**
  * Interpreter for [[algebra.BasicAuthentication]] that produces
  * OpenAPI documentation.
  *
  * @group interpreters
  */
trait BasicAuthentication
  extends algebra.BasicAuthentication
    with Endpoints {

  private[endpoints] lazy val basicAuthenticationHeader: RequestHeaders[Credentials] = header("Authorization")

  private[endpoints] def authenticated[A](response: Response[A], docs: Documentation): Response[Option[A]] =
    DocumentedResponse(401, docs.getOrElse(""), content = Map.empty) :: response

}
