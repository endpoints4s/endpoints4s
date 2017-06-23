package endpoints
package openapi

/**
  * Interpreter for [[algebra.OptionalResponses]]
  */
trait DocumentedOptionalResponses
  extends algebra.DocumentedOptionalResponses
    with DocumentedEndpoints {

  def option[A](response: Response[A], description: String): Response[Option[A]] =
    DocumentedResponse(404, description, content = Map.empty) :: response

}
