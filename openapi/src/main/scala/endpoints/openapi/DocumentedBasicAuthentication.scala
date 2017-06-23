package endpoints
package openapi

import endpoints.algebra.BasicAuthentication.Credentials

/**
  * Interpreter for [[algebra.BasicAuthentication]] that produces
  * OpenAPI documentation.
  */
trait DocumentedBasicAuthentication
  extends algebra.DocumentedBasicAuthentication
    with DocumentedEndpoints {

  private[endpoints] def basicAuthentication: RequestHeaders[Credentials] =
    DocumentedHeaders("Authorization" :: Nil)

  private[endpoints] def authenticated[A](response: Response[A], description: String): Response[Option[A]] =
    DocumentedResponse(401, description, content = Map.empty) :: response

}
