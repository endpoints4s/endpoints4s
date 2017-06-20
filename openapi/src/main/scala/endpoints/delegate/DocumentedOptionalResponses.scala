package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedOptionalResponses]] that ignores
  * information related to documentation and delegates to another
  * [[algebra.OptionalResponses]] interpreter.
  */
trait DocumentedOptionalResponses
  extends algebra.DocumentedOptionalResponses
    with DocumentedEndpoints {

  val delegate: algebra.OptionalResponses

  def option[A](response: Response[A], description: String): Response[Option[A]] =
    delegate.option(response)

}
