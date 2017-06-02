package endpoints
package algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing endpoints including documentation
  * (such as human readable descriptions of things).
  *
  * This interface is modeled after [[Endpoints]] but some methods
  * take additional parameters carrying the documentation part.
  */
trait DocumentedEndpoints extends DocumentedUrls with Methods {

  /** Information carried by a whole request (headers and entity) */
  type Request[A]

  /**
    * Request for given parameters
    *
    * @param method Request method
    * @param url Request URL
    */
  def request[A](
    method: Method,
    url: Url[A]
  ): Request[A]

  /**
    * Helper method to perform GET request
    */
  final def get[A](url: Url[A]): Request[A] = request(Get, url)

  /** Information carried by a response */
  type Response[A]

  /**
    * Empty response.
    */
  def emptyResponse(description: String): Response[Unit]

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
