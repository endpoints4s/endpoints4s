package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.Responses]] that ignores information
  * related to documentation and delegates to another [[endpoints.algebra.Responses]]
  * interpreter.
  */
trait Responses extends algebra.Responses {

  val delegate: endpoints.algebra.Responses

  type Response[A] = delegate.Response[A]

  def emptyResponse(documentation: String): Response[Unit] = delegate.emptyResponse

  def textResponse(documentation: String): Response[String] = delegate.textResponse

}
