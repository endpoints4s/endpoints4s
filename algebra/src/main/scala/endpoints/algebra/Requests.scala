package endpoints.algebra

import endpoints.Tupler

import scala.language.higherKinds

trait Requests extends Urls with Methods {

  /** Information carried by requests’ headers */
  type RequestHeaders[A]

  /**
    * No particular information. Does not mean that the headers *have to*
    * be empty. Just that, from a server point of view no information will
    * be extracted from them, and from a client point of view no particular
    * headers will be built in the request.
    */
  def emptyHeaders: RequestHeaders[Unit]

  /** Join informations carried by two sets of headers */
  def joinHeaders[H1, H2](h1: RequestHeaders[H1], h2: RequestHeaders[H2])(implicit tupler: Tupler[H1, H2]): RequestHeaders[tupler.Out]


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
    * @param entity Request entity
    * @param headers Request headers
    */
  def request[U, E, H, UE](
    method: Method,
    url: Url[U],
    entity: RequestEntity[E] = emptyRequest,
    headers: RequestHeaders[H] = emptyHeaders
  )(implicit tuplerUE: Tupler.Aux[U, E, UE], tuplerUEH: Tupler[UE, H]): Request[tuplerUEH.Out]

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
