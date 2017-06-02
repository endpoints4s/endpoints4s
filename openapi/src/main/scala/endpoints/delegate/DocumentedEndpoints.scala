package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedEndpoints]] that ignores information
  * related to documentation and delegates to another [[algebra.Endpoints]]
  * interpreter.
  */
trait DocumentedEndpoints
  extends algebra.DocumentedEndpoints
    with DocumentedUrls
    with Methods {

  val delegate: algebra.Endpoints

  type Request[A] = delegate.Request[A]

  type Response[A] = delegate.Response[A]

  type Endpoint[A, B] = delegate.Endpoint[A, B]

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    delegate.endpoint(request, response)

  def emptyResponse(description: String): Response[Unit] = delegate.emptyResponse

  def request[A](method: Method, url: Url[A]): Request[A] =
    delegate.request[A, Unit, Unit, A](method, url)

}
