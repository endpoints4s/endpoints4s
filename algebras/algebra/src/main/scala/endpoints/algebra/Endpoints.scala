package endpoints.algebra

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
  * This trait uses [[BuiltInErrors]] to model client and server errors.
  *
  * @group algebras
  */
trait Endpoints extends EndpointsWithCustomErrors with BuiltInErrors

/**
  * Algebra interface for describing endpoints made of requests and responses.
  *
  * @group algebras
  */
trait EndpointsWithCustomErrors extends Requests with Responses with Errors {

  /**
    * Information carried by an HTTP endpoint
    *
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    * @group types
    */
  type Endpoint[A, B]

  /**
    * HTTP endpoint.
    *
    * @param request  Request
    * @param response Response
    * @param docs     Documentation (used by documentation interpreters)
    * @group operations
    */
  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B]

  /**
    * @param summary     Short description
    * @param description Detailed description
    * @param tags        OpenAPI tags
    * @param callbacks   Callbacks indexed by event name
    * @param deprecated  Indicates whether this endpoint is deprecated or not
    */
  case class EndpointDocs(
      summary: Documentation = None,
      description: Documentation = None,
      tags: List[String] = Nil,
      callbacks: Map[String, CallbacksDocs] = Map.empty,
      deprecated: Boolean = false
  )

  /**
    * Callbacks indexed by URL pattern
    * @see Swagger Documentation at [[https://swagger.io/docs/specification/callbacks/]]
    */
  type CallbacksDocs = Map[String, CallbackDocs]

  /**
    * @param method   HTTP method used for the callback
    * @param entity   Contents of the callback message
    * @param response Expected response
    */
  case class CallbackDocs(
      method: Method,
      entity: RequestEntity[_],
      response: Response[_],
      requestDocs: Documentation = None
  )

}
