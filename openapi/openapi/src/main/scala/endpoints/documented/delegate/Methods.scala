package endpoints
package documented
package delegate

/**
  * Interpreter for [[endpoints.algebra.Methods]] that delegates to
  * another interpreter.
  */
trait Methods extends endpoints.algebra.Methods {

  val delegate: endpoints.algebra.Methods

  type Method = delegate.Method

  def Get: Method = delegate.Get

  def Post: Method = delegate.Post

  def Put: Method = delegate.Put

  def Delete: Method = delegate.Delete

  def Patch: Method = delegate.Patch

}
