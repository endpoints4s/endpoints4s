package endpoints
package documented
package openapi

/**
  * Interpreter for [[algebra.OptionalResponses]]
  */
trait OptionalResponses
  extends algebra.OptionalResponses
    with Endpoints {

  def option[A](response: Response[A], description: String): Response[Option[A]] =
    DocumentedResponse(404, description, content = Map.empty) :: response

}
