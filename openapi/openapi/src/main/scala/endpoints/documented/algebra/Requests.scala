package endpoints
package documented
package algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing requests.
  *
  * This interface is modeled after [[endpoints.algebra.Requests]] but some
  * methods take additional parameters carrying documentation.
  */
trait Requests extends Urls with endpoints.algebra.Methods {

  /** Information carried by requests’ headers */
  type RequestHeaders[A]

  /**
    * No particular information. Does not mean that the headers *have to*
    * be empty. Just that, from a server point of view no information will
    * be extracted from them, and from a client point of view no particular
    * headers will be built in the request.
    */
  def emptyHeaders: RequestHeaders[Unit]

  /** Information carried by a whole request (headers and entity) */
  type Request[A]

  /** Information carried by request entity */
  type RequestEntity[A]

  /**
    * Empty request.
    */
  def emptyRequest: RequestEntity[Unit]

  /**
    * Request for given parameters
    *
    * @param method Request method
    * @param url Request URL
    */
  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out]

  /**
    * Helper method to perform GET request
    */
  final def get[A, B](
    url: Url[A],
    headers: RequestHeaders[B] = emptyHeaders
  )(implicit tuplerAC: Tupler[A, B]): Request[tuplerAC.Out] = request(Get, url, headers = headers)

  /**
    * Helper method to perform POST request
    */
  final def post[A, B, C, AB](
    url: Url[A],
    entity: RequestEntity[B],
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] = request(Post, url, entity, headers)

}
