package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.OptionalResponses]] that ignores
  * information related to documentation and delegates to another
  * [[endpoints.algebra.OptionalResponses]] interpreter.
  */
trait OptionalResponses
  extends algebra.OptionalResponses
    with Endpoints {

  val delegate: endpoints.algebra.OptionalResponses

  def option[A](response: Response[A], notFoundDocumentation: String): Response[Option[A]] =
    delegate.option(response)

}
