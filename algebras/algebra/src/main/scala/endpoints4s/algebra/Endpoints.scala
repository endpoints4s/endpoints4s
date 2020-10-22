package endpoints4s.algebra

import endpoints4s.Hashing

import scala.annotation.nowarn

/** Algebra interface for describing endpoints made of requests and responses.
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

/** Algebra interface for describing endpoints made of requests and responses.
  *
  * @group algebras
  */
trait EndpointsWithCustomErrors extends Requests with Responses with Errors {

  /** Information carried by an HTTP endpoint
    *
    * Values of type [[Endpoint]] can be constructed by using the operation
    * [[endpoint]].
    *
    * @tparam A Information carried by the request
    * @tparam B Information carried by the response
    * @group types
    */
  type Endpoint[A, B]

  /** Define an HTTP endpoint
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

  /** @param operationId A unique identifier which identifies this operation
    * @param summary     Short description
    * @param description Detailed description
    * @param tags        OpenAPI tags
    * @param callbacks   Callbacks indexed by event name
    * @param deprecated  Indicates whether this endpoint is deprecated or not
    */
  final class EndpointDocs private (
      val operationId: Option[String],
      val summary: Documentation,
      val description: Documentation,
      val tags: List[Tag],
      val callbacks: Map[String, CallbacksDocs],
      val deprecated: Boolean
  ) extends Serializable {

    override def toString =
      s"EndpointDocs($operationId, $summary, $description, $tags, $callbacks, $deprecated)"

    @nowarn("cat=unchecked")
    override def equals(other: Any): Boolean =
      other match {
        case that: EndpointDocs =>
          operationId == that.operationId &&
            summary == that.summary &&
            description == that.description &&
            tags == that.tags &&
            callbacks == that.callbacks &&
            deprecated == that.deprecated
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(
        operationId,
        summary,
        description,
        tags,
        callbacks,
        deprecated
      )

    private[this] def copy(
        operationId: Option[String] = operationId,
        summary: Documentation = summary,
        description: Documentation = description,
        tags: List[Tag] = tags,
        callbacks: Map[String, CallbacksDocs] = callbacks,
        deprecated: Boolean = deprecated
    ): EndpointDocs =
      new EndpointDocs(
        operationId,
        summary,
        description,
        tags,
        callbacks,
        deprecated
      )

    def withOperationId(operationId: Option[String]): EndpointDocs =
      copy(operationId = operationId)

    def withSummary(summary: Documentation): EndpointDocs =
      copy(summary = summary)

    def withDescription(description: Documentation): EndpointDocs =
      copy(description = description)

    def withTags(tags: List[Tag]): EndpointDocs =
      copy(tags = tags)

    def withCallbacks(callbacks: Map[String, CallbacksDocs]): EndpointDocs =
      copy(callbacks = callbacks)

    def withDeprecated(deprecated: Boolean): EndpointDocs =
      copy(deprecated = deprecated)

  }

  object EndpointDocs {

    /** @return An empty documentation value, with no summary, no description,
      *         no tags, no callbacks, and the `deprecated` flag set to `false`.
      *
      * You can transform the returned [[EndpointDocs]] value by using the `withXxx`
      * operations:
      *
      * {{{
      *   EndpointDocs().withSummary(Some("endpoint summary"))
      * }}}
      */
    def apply(): EndpointDocs =
      new EndpointDocs(None, None, None, Nil, Map.empty, false)

    @deprecated(
      "Use `EndpointDocs().withXxx(...)` instead of `EndpointDocs(xxx = ...)`",
      "1.0.0"
    )
    def apply(
        summary: Documentation = None,
        description: Documentation = None,
        tags: List[String] = Nil,
        callbacks: Map[String, CallbacksDocs] = Map.empty,
        deprecated: Boolean = false
    ): EndpointDocs =
      new EndpointDocs(
        None,
        summary,
        description,
        tags.map(Tag(_)),
        callbacks,
        deprecated
      )

  }

  /** Callbacks indexed by URL pattern
    * @see Swagger Documentation at [[https://swagger.io/docs/specification/callbacks/]]
    */
  type CallbacksDocs = Map[String, CallbackDocs]

  /** @param method   HTTP method used for the callback
    * @param entity   Contents of the callback message
    * @param response Expected response
    */
  final class CallbackDocs private (
      val method: Method,
      val entity: CallbackDocs.SomeRequestEntity,
      val response: CallbackDocs.SomeResponse,
      val requestDocs: Documentation
  ) extends Serializable {

    override def toString =
      s"CallbackDocs($method, $entity, $response, $requestDocs)"

    @nowarn("cat=unchecked")
    override def equals(other: Any): Boolean =
      other match {
        case that: CallbackDocs =>
          method == that.method &&
            entity == that.entity &&
            response == that.response &&
            requestDocs == that.requestDocs
        case _ => false
      }

    override def hashCode(): Int =
      Hashing.hash(method, entity, response, requestDocs)

    private[this] def copy(
        method: Method = method,
        entity: CallbackDocs.SomeRequestEntity = entity,
        response: CallbackDocs.SomeResponse = response,
        requestDocs: Documentation = requestDocs
    ): CallbackDocs =
      new CallbackDocs(method, entity, response, requestDocs)

    def withMethod(method: Method): CallbackDocs =
      copy(method = method)

    def withRequestEntity[A](entity: RequestEntity[A]): CallbackDocs =
      copy(entity = CallbackDocs.SomeRequestEntity(entity))

    def withRequestDocs(requestDocs: Documentation): CallbackDocs =
      copy(requestDocs = requestDocs)

    def withResponse[A](response: Response[A]): CallbackDocs =
      copy(response = CallbackDocs.SomeResponse(response))

  }

  object CallbackDocs {

    /** A wrapper type for a [[RequestEntity]] whose carried information is unknown.
      *
      * This wrapper type is necessary because Scala 3 does not support writing `RequestEntity[_]`.
      */
    trait SomeRequestEntity {
      type T
      def value: RequestEntity[T]
    }

    object SomeRequestEntity {
      def apply[A](requestEntity: RequestEntity[A]): SomeRequestEntity =
        new SomeRequestEntity {
          type T = A
          def value: RequestEntity[T] = requestEntity
        }
    }

    /** A wrapper type for a [[Response]] whose carried information is unknown.
      *
      * This wrapper type is necessary because Scala 3 does not support writing `Response[_]`
      */
    trait SomeResponse {
      type T
      def value: Response[T]
    }

    object SomeResponse {
      def apply[A](response: Response[A]): SomeResponse =
        new SomeResponse {
          type T = A
          def value: Response[T] = response
        }
    }

    /** Convenience constructor that wraps the `entity` and `response` parameters.
      */
    def apply[A, B](
        method: Method,
        entity: RequestEntity[A],
        response: Response[B]
    ): CallbackDocs =
      new CallbackDocs(
        method,
        SomeRequestEntity(entity),
        SomeResponse(response),
        None
      )

    @deprecated(
      "Use `CallbackDocs(...).withRequestDocs(docs)` instead of `CallbackDocs(..., requestDocs = docs)`",
      "1.0.0"
    )
    def apply[A, B](
        method: Method,
        entity: RequestEntity[A],
        response: Response[B],
        requestDocs: Documentation = None
    ): CallbackDocs =
      new CallbackDocs(
        method,
        SomeRequestEntity(entity),
        SomeResponse(response),
        requestDocs
      )

  }

}
