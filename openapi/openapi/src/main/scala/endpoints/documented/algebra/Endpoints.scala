package endpoints
package documented
package algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing endpoints including documentation
  * (such as human readable descriptions of things).
  *
  * This interface is modeled after [[endpoints.algebra.Endpoints]] but some methods
  * take additional parameters carrying the documentation part.
  */
trait Endpoints
  extends Requests
    with Responses {

  /**
    * Information carried by an HTTP endpoint
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    */
  type Endpoint[A, B]

  /**
    * HTTP endpoint.
    *
    * @param request Request
    * @param response Response
    */
  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

}
