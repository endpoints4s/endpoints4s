package endpoints.algebra

import scala.language.higherKinds

/**
  * Algebra interface for describing endpoints made of requests and responses.
  *
  * Requests and responses contain headers and entity.
  *
  * {{{
  *   /**
  *     * Describes an HTTP endpoint whose:
  *     *  - request uses verb “GET”,
  *     *  - URL is made of path “/foo”,
  *     *  - response has no entity
  *     */
  *   val example = endpoint(get(path / "foo"), emptyResponse)
  * }}}
  *
  * @group algebras
  */
trait Endpoints extends Requests with Responses {

  /**
    * Information carried by an HTTP endpoint
    *
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    */
  type Endpoint[A, B]

  /**
    * HTTP endpoint.
    *
    * @param request  Request
    * @param response Response
    * @param summary optional summary documentation
    * @param description optional description documentation
    * @param tags list of OpenApi tags
    */
  def endpoint[A, B](
    request: Request[A],
    response: Response[B],
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): Endpoint[A, B]

}
