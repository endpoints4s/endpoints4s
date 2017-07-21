package endpoints
package documented
package delegate

import endpoints.algebra.BasicAuthentication.Credentials

/**
  * Interpreter for [[algebra.BasicAuthentication]] that delegates to
  * another [[endpoints.algebra.BasicAuthentication]] interpreter.
  */
trait BasicAuthentication
  extends algebra.BasicAuthentication
    with Endpoints {

  val delegate: endpoints.algebra.BasicAuthentication

  private[endpoints] def basicAuthentication: RequestHeaders[Credentials] =
    delegate.basicAuthentication

  private[endpoints] def authenticated[A](response: Response[A], documentation: String): Response[Option[A]] =
    delegate.authenticated(response)

}
