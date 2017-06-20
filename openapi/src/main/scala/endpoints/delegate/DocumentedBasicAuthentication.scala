package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedBasicAuthentication]] that delegates to
  * another [[algebra.BasicAuthentication]] interpreter.
  */
trait DocumentedBasicAuthentication
  extends algebra.DocumentedBasicAuthentication
    with DocumentedEndpoints {

  val delegate: algebra.BasicAuthentication

  private[endpoints] def basicAuthentication: RequestHeaders[algebra.BasicAuthentication.Credentials] =
    delegate.basicAuthentication

  private[endpoints] def authenticated[A](response: Response[A], description: String): Response[Option[A]] =
    delegate.authenticated(response)

}
