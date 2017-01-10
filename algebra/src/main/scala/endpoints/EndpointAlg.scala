package endpoints

import scala.language.higherKinds

/**
  * Algebra interface for defining endpoints made of requests and responses.
  *
  * Requests and responses contain headers and entity.
  */
trait EndpointAlg extends UrlAlg with MethodAlg {

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
    * @param entity Request entity
    * @param headers Request headers
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
  def get[A, B](
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


  /** Information carried by a response */
  type Response[A]

  /**
    * Empty response.
    */
  def emptyResponse: Response[Unit]

  /**
    * Information carried by an HTTP endpoint
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    */
  type Endpoint[A, B]

  /**
    * HTTP endpint.
    *
    * @param request Request
    * @param response Response
    */
  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B]

}
