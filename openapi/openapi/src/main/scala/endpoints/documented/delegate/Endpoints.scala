package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.Endpoints]] that ignores information
  * related to documentation and delegates to another [[endpoints.algebra.Endpoints]]
  * interpreter.
  */
trait Endpoints
  extends algebra.Endpoints
    with Requests
    with Responses {

  val delegate: endpoints.algebra.Endpoints

  type Endpoint[A, B] = delegate.Endpoint[A, B]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    delegate.endpoint(request, response)

}
