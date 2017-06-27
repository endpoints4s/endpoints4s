package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedResponses]] that ignores information
  * related to documentation and delegates to another [[algebra.Responses]]
  * interpreter.
  */
trait DocumentedResponses extends algebra.DocumentedResponses {

  val delegate: algebra.Responses

  type Response[A] = delegate.Response[A]

  def emptyResponse(description: String): Response[Unit] = delegate.emptyResponse

  def textResponse(description: String): Response[String] = delegate.textResponse

}
