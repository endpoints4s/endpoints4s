package endpoints
package documented
package openapi

/**
  * Interpreter for [[algebra.OptionalResponses]]
  */
trait OptionalResponses
  extends algebra.OptionalResponses
    with Endpoints {

  def option[A](response: Response[A], notFoundDocumentation: String): Response[Option[A]] =
    DocumentedResponse(404, notFoundDocumentation, content = Map.empty) :: response

}
