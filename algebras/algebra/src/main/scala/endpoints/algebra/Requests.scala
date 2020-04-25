package endpoints.algebra

import endpoints._

/**
  * @group algebras
  */
trait Requests extends Urls with Methods with SemigroupalSyntax {

  /** Information carried by requests’ headers.
    *
    * You can construct values of type `RequestHeaders` by using the operations
    * [[requestHeader]], [[optRequestHeader]], or [[emptyRequestHeaders]].
    *
    *   - Server interpreters raise an error if they can’t parse the incoming
    *     request headers as a value of type `A`. By default,
    *     they produce a Bad Request (400) response with a list of error messages
    *     in a JSON array. Refer to the documentation of your server interpreter
    *     to customize this behavior.
    *
    * @note  This type has implicit methods provided by the [[SemigroupalSyntax]]
    *        and [[PartialInvariantFunctorSyntax]] classes.
    * @group types */
  type RequestHeaders[A]

  /** Ignore headers
    *
    *   - Server interpreters don’t try to parse any information from the
    *     request headers,
    *   - Client interpreters supply no specific headers
    *
    * Use `description` of [[endpoints.algebra.Endpoints#endpoint]] to document empty headers.
    * @group operations
    */
  def emptyRequestHeaders: RequestHeaders[Unit]

  /**
    * A required request header
    * @param name Header name (e.g., “Authorization”)
    * @group operations
    */
  def requestHeader(
      name: String,
      docs: Documentation = None
  ): RequestHeaders[String]

  /**
    * An optional request header
    * @param name Header name (e.g., “Authorization”)
    * @group operations
    */
  def optRequestHeader(
      name: String,
      docs: Documentation = None
  ): RequestHeaders[Option[String]]

  /** Provides `++` operation.
    * @see [[SemigroupalSyntax]] */
  implicit def requestHeadersSemigroupal: Semigroupal[RequestHeaders]

  /** Provides the operations `xmap` and `xmapPartial`.
    * @see [[PartialInvariantFunctorSyntax]] */
  implicit def requestHeadersPartialInvariantFunctor
      : PartialInvariantFunctor[RequestHeaders]

  /** Information carried by a whole request (headers and entity)
    *
    * Values of type `Request[A]` can be constructed by using the operations
    * [[request]], [[get]], [[post]], [[put]], or [[delete]].
    *
    *   - Server interpreters raise an error if they can’t parse the incoming
    *     request as a value of type `A`. By default,
    *     they produce a Bad Request (400) response with a list of error messages
    *     in a JSON array. Refer to the documentation of your server interpreter
    *     to customize this behavior.
    *
    * @note This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]] class.
    * @group types */
  type Request[A]

  /** Provides the operations `xmap` and `xmapPartial`.
    * @see [[PartialInvariantFunctorSyntax]] */
  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request]

  /** Information carried by request entity
    *
    * Values of type `RequestEntity[A]` can be constructed by using the operations
    * [[emptyRequest]] or [[textRequest]]. Additional types of request entities
    * are provided by other algebra modules, such as [[JsonEntities]] or [[ChunkedEntities]].
    *
    *   - Server interpreters raise an error if they can’t parse the incoming
    *     request entity as a value of type `A`. By default,
    *     they produce a Bad Request (400) response with a list of error messages
    *     in a JSON array. Refer to the documentation of your server interpreter
    *     to customize this behavior.
    *
    * @note  This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]] class.
    * @group types */
  type RequestEntity[A]

  /** Provides the operations `xmap` and `xmapPartial`.
    * @see [[PartialInvariantFunctorSyntax]] */
  implicit def requestEntityPartialInvariantFunctor
      : PartialInvariantFunctor[RequestEntity]

  /**
    * Empty request -- request without a body.
    * Use `description` of [[endpoints.algebra.Endpoints#endpoint]] to document an empty body.
    * @group operations
    */
  def emptyRequest: RequestEntity[Unit]

  /**
    * Request with a `String` body.
    * @group operations
    */
  def textRequest: RequestEntity[String]

  /**
    * Request for given parameters
    *
    * @param method Request method
    * @param url Request URL
    * @param entity Request entity
    * @param docs Request documentation
    * @param headers Request headers
    * @tparam UrlP Payload carried by url
    * @tparam BodyP Payload carried by body
    * @tparam HeadersP Payload carried by headers
    * @tparam UrlAndBodyPTupled Payloads of Url and Body tupled together by [[Tupler]]
    * @group operations
    */
  def request[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      method: Method,
      url: Url[UrlP],
      entity: RequestEntity[BodyP] = emptyRequest,
      docs: Documentation = None,
      headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(
      implicit tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
      tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]
  ): Request[Out]

  /**
    * Helper method to perform GET request
    * @tparam UrlP Payload carried by url
    * @tparam HeadersP Payload carried by headers
    * @group operations
    */
  final def get[UrlP, HeadersP, Out](
      url: Url[UrlP],
      docs: Documentation = None,
      headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(implicit tuplerUH: Tupler.Aux[UrlP, HeadersP, Out]): Request[Out] =
    request(Get, url, docs = docs, headers = headers)

  /**
    * Helper method to perform POST request
    * @param docs Request documentation
    * @tparam UrlP Payload carried by url
    * @tparam BodyP Payload carried by body
    * @tparam HeadersP Payload carried by headers
    * @tparam UrlAndBodyPTupled Payloads of Url and Body tupled together by [[Tupler]]
    * @group operations
    */
  final def post[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      url: Url[UrlP],
      entity: RequestEntity[BodyP],
      docs: Documentation = None,
      headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(
      implicit tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
      tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]
  ): Request[Out] =
    request(Post, url, entity, docs, headers)

  /**
    * Helper method to perform PUT request
    * @tparam UrlP Payload carried by url
    * @tparam BodyP Payload carried by body
    * @tparam HeadersP Payload carried by headers
    * @tparam UrlAndBodyPTupled Payloads of Url and Body tupled together by [[Tupler]]
    * @group operations
    */
  final def put[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      url: Url[UrlP],
      entity: RequestEntity[BodyP],
      docs: Documentation = None,
      headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(
      implicit tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
      tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]
  ): Request[Out] =
    request(Put, url, entity, docs, headers)

  /**
    * Helper method to perform DELETE request
    * @tparam UrlP Payload carried by url
    * @tparam HeadersP Payload carried by headers
    * @group operations
    */
  final def delete[UrlP, HeadersP, Out](
      url: Url[UrlP],
      docs: Documentation = None,
      headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(implicit tuplerUH: Tupler.Aux[UrlP, HeadersP, Out]): Request[Out] =
    request(Delete, url, docs = docs, headers = headers)

  /**
    * Helper method to perform PATCH request
    * @param docs Request documentation
    * @tparam UrlP Payload carried by url
    * @tparam BodyP Payload carried by body
    * @tparam HeadersP Payload carried by headers
    * @tparam UrlAndBodyPTupled Payloads of Url and Body tupled together by [[Tupler]]
    * @group operations
    */
  final def patch[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      url: Url[UrlP],
      entity: RequestEntity[BodyP],
      docs: Documentation = None,
      headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(
      implicit tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
      tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]
  ): Request[Out] =
    request(Patch, url, entity, docs, headers)

}
