package endpoints
package delegate

/**
  * Interpreter for [[algebra.Methods]] that delegates to
  * another interpreter.
  */
trait Methods extends algebra.Methods {

  val delegate: algebra.Methods

  type Method = delegate.Method

  def Get: Method = delegate.Get

  def Post: Method = delegate.Post

  def Put: Method = delegate.Put

  def Delete: Method = delegate.Delete

}
