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
    * Values of type [[Endpoint]] can be constructed by using the operation
    * [[endpoint]].
    *
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    * @group types
    */
  type Endpoint[A, B]

  /**
    * Define an HTTP endpoint
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
  case class CallbackDocs private (
      method: Method,
      entity: CallbackDocs.SomeRequestEntity,
      response: CallbackDocs.SomeResponse,
      requestDocs: Documentation
  )

  object CallbackDocs {

    /**
      * A wrapper type for a [[RequestEntity]] whose carried information is unknown.
      *
      * This wrapper type is necessary because Scala 3 does not support writing `RequestEntity[_]`.
      */
    trait SomeRequestEntity {
      type T
      def value: RequestEntity[T]
    }

    /**
      * A wrapper type for a [[Response]] whose carried information is unknown.
      *
      * This wrapper type is necessary because Scala 3 does not support writing `Response[_]`
      */
    trait SomeResponse {
      type T
      def value: Response[T]
    }

    /**
      * Convenience constructor that wraps the `entity` and `response` parameters.
      */
    def apply[A, B](
        method: Method,
        entity: RequestEntity[A],
        response: Response[B],
        requestDocs: Documentation = None
    ): CallbackDocs =
      new CallbackDocs(
        method,
        new SomeRequestEntity { type T = A; def value = entity },
        new SomeResponse { type T = B; def value = response },
        requestDocs
      )
  }

}
