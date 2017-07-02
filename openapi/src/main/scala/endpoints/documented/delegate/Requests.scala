package endpoints
package documented
package delegate

/**
  * Interpreter for [[algebra.Requests]] that ignores information
  * related to documentation and delegates to another [[endpoints.algebra.Requests]]
  * interpreter.
  */
trait Requests
  extends algebra.Requests
    with Urls
    with Methods {

  val delegate: endpoints.algebra.Requests

  type RequestHeaders[A] = delegate.RequestHeaders[A]

  def emptyHeaders: RequestHeaders[Unit] = delegate.emptyHeaders

  type Request[A] = delegate.Request[A]

  type RequestEntity[A] = delegate.RequestEntity[A]

  def emptyRequest: RequestEntity[Unit] = delegate.emptyRequest

  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    delegate.request(method, url, entity, headers)

}
