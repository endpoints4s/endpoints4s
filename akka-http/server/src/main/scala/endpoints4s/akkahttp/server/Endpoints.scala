package endpoints4s.akkahttp.server

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpRequest, MediaTypes, Uri}
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling._
import endpoints4s.algebra.Documentation
import endpoints4s._

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/** Interpreter for [[algebra.Endpoints]] that performs routing using Akka-HTTP and uses [[algebra.BuiltInErrors]]
  * to model client and server errors.
  *
  * @group interpreters
  */
trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

/** Interpreter for [[algebra.Endpoints]] that performs routing using Akka-HTTP.
  * @group interpreters
  */
trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with algebra.internal.RequestData
    with Urls
    with Methods
    with StatusCodes {

  trait RequestHeaders[A] {
    def decode(httpRequest: HttpRequest): Validated[A]
  }

  trait Request[A] extends RequestData[A] {

    /** A directive that extracts an `A` from an incoming request */
    final lazy val directive: Directive1[A] =
      matchAndParseHeadersDirective.flatMap {
        case invalid: Invalid => handleClientErrors(invalid)
        case Valid((urlData, headersData)) =>
          entity.flatMap { entityData =>
            val validated = aggregateAndValidate(urlData, entityData, headersData)
            validated match {
              case Valid(value)     => Directives.provide(value)
              case invalid: Invalid => handleClientErrors(invalid)
            }
          }
      }

    /** Checks whether the incoming request matches this request description, and
      * if this is the case, parses its URL parameters and headers.
      *
      * The directive produces:
      *
      *   - a ''rejection'' to signal that the incoming request does not match
      *     this request description,
      *   - a ''completion'' to immediately return a custom response (e.g. 401),
      *   - a value `Valid(urlAndHeadersData)` in case the URL and headers were
      *     successfully parsed,
      *   - a value `Invalid(errors)` in case the URL and headers had validation
      *     errors.
      */
    private[server] def matchAndParseHeadersDirective: Directive1[Validated[(UrlData, HeadersData)]]

    /** The URI of a request carrying the given `a` parameter */
    final def uri(a: A): Uri = url.uri(urlData(a))

    private[server] def aggregateAndValidate(
        urlData: UrlData,
        entityData: EntityData,
        headersData: HeadersData
    ): Validated[A]
  }

  // Default implementation for `matchAndParseHeadersDirective`, which always
  // provides the parsed URL and headers, if the incoming request matches
  // the request description (otherwise, the directive produces a rejection).
  private[server] final def matchAndProvideParsedUrlAndHeadersData[U, H](
      method: Method,
      url: Url[U],
      headers: RequestHeaders[H]
  ): Directive1[Validated[(U, H)]] = {
    val methodDirective = convToDirective1(Directives.method(method))
    val headersDirective = Directives.extractRequest.map(headers.decode)
    (methodDirective & url.directive)
      .tflatMap { case (_, validatedQuery) =>
        headersDirective.flatMap { validatedHeaders =>
          Directives.provide(validatedQuery.zip(validatedHeaders))
        }
      }
  }

  type RequestEntity[A] = Directive1[A]

  type ResponseEntity[A] = ToEntityMarshaller[A]

  type ResponseHeaders[A] = A => collection.immutable.Seq[HttpHeader]

  type Response[A] = A => Route

  implicit lazy val responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {

      def xmap[A, B](
          fa: Response[A],
          f: A => B,
          g: B => A
      ): Response[B] =
        fa compose g
    }

  implicit lazy val responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new InvariantFunctor[ResponseEntity] {

      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] = fa compose g
    }

  private[server] val endpointsExceptionHandler =
    ExceptionHandler { case NonFatal(t) => handleServerError(t) }

  case class Endpoint[A, B](request: Request[A], response: Response[B]) {

    /** @return An Akka HTTP `Route` for this endpoint
      * @param implementation Function that transforms the `A` value carried in
      *                       the request into a `B` value to send in the response.
      */
    def implementedBy(implementation: A => B): Route =
      Directives.handleExceptions(endpointsExceptionHandler) {
        request.directive { arguments =>
          Directives.encodeResponse {
            response(implementation(arguments))
          }
        }
      }

    /** @return An Akka HTTP `Route` for this endpoint
      * @param implementation Asynchronous function that transforms the `A` value
      *                       carried in the request into a `B` value to send in
      *                       the response.
      */
    def implementedByAsync(implementation: A => Future[B]): Route =
      Directives.handleExceptions(endpointsExceptionHandler) {
        request.directive { arguments =>
          Directives.onComplete(implementation(arguments)) {
            case Success(result) => Directives.encodeResponse(response(result))
            case Failure(ex)     => throw ex
          }
        }
      }

    /** @return The `Uri` of this endpoint, for a request carrying the
      *         given `a` value.
      */
    def uri(a: A): Uri = request.uri(a)
  }

  /* ************************
      REQUESTS
  ************************* */

  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {

      def xmapPartial[A, B](fa: Request[A], f: A => Validated[B], g: B => A): Request[B] =
        new Request[B] {
          type UrlData = fa.UrlData
          type HeadersData = fa.HeadersData
          type EntityData = fa.EntityData

          def urlData(b: B): UrlData = fa.urlData(g(b))
          def url: Url[UrlData] = fa.url
          def method: Method = fa.method

          def entity: RequestEntity[EntityData] = fa.entity
          def headers: RequestHeaders[HeadersData] = fa.headers
          def aggregateAndValidate(
              urlData: fa.UrlData,
              entityData: fa.EntityData,
              headersData: fa.HeadersData
          ): Validated[B] = fa.aggregateAndValidate(urlData, entityData, headersData).flatMap(f)

          private[server] lazy val matchAndParseHeadersDirective
              : Directive1[Validated[(UrlData, HeadersData)]] =
            matchAndProvideParsedUrlAndHeadersData(method, url, headers)
        }
    }

  def emptyRequest: RequestEntity[Unit] = convToDirective1(Directives.pass)

  def textRequest: RequestEntity[String] = {
    implicit val um: FromEntityUnmarshaller[String] =
      Unmarshaller.stringUnmarshaller
        .forContentTypes(MediaTypes.`text/plain`)
    Directives.entity[String](implicitly)
  }

  def choiceRequestEntity[A, B](
      requestEntityA: Directive1[A],
      requestEntityB: Directive1[B]
  ): Directive1[Either[A, B]] = {
    val requestEntityAAsEither = requestEntityA.map(Left(_): Either[A, B])
    val requestEntityBAsEither = requestEntityB.map(Right(_): Either[A, B])

    requestEntityAAsEither | requestEntityBAsEither
  }

  implicit lazy val requestEntityPartialInvariantFunctor: PartialInvariantFunctor[RequestEntity] =
    directive1InvFunctor

  /* ************************
      HEADERS
  ************************* */

  def emptyRequestHeaders: RequestHeaders[Unit] =
    _ => Valid(())

  def requestHeader(name: String, docs: Documentation): RequestHeaders[String] =
    httpRequest =>
      httpRequest.headers.find(_.lowercaseName() == name.toLowerCase) match {
        case Some(header) => Valid(header.value())
        case None         => Invalid(s"Missing header $name")
      }

  def optRequestHeader(
      name: String,
      docs: Documentation
  ): RequestHeaders[Option[String]] =
    httpRequest =>
      httpRequest.headers.find(_.lowercaseName() == name.toLowerCase) match {
        case Some(header) => Valid(Some(header.value()))
        case None         => Valid(None)
      }

  implicit lazy val requestHeadersPartialInvariantFunctor: PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {

      def xmapPartial[A, B](
          fa: RequestHeaders[A],
          f: A => Validated[B],
          g: B => A
      ): RequestHeaders[B] =
        headers => fa.decode(headers).flatMap(f)
    }

  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {

      def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): RequestHeaders[tupler.Out] =
        (httpRequest: HttpRequest) => fa.decode(httpRequest).zip(fb.decode(httpRequest))(tupler)
    }

  /* ************************
      RESPONSES
  ************************* */

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {

      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        out => {
          val (a, b) = tupler.unapply(out)
          fa(a) ++ fb(b)
        }
    }

  implicit def responseHeadersInvariantFunctor: InvariantFunctor[ResponseHeaders] =
    new InvariantFunctor[ResponseHeaders] {

      def xmap[A, B](
          fa: ResponseHeaders[A],
          f: A => B,
          g: B => A
      ): ResponseHeaders[B] =
        fa compose g
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] = _ => Nil

  def responseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[String] =
    value => RawHeader(name, value) :: Nil

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] = {
    case Some(value) => RawHeader(name, value) :: Nil
    case None        => Nil
  }

  def emptyResponse: ResponseEntity[Unit] =
    Marshaller.opaque(_ => HttpEntity.Empty)

  def textResponse: ResponseEntity[String] = implicitly

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B] = emptyResponseHeaders
  )(implicit
      tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    r => {
      val (a, b) = tupler.unapply(r)
      val httpHeaders = headers(b)
      implicit val marshaller: ToResponseMarshaller[A] =
        Marshaller.fromToEntityMarshaller(statusCode, httpHeaders)(entity)
      Directives.complete(a)
    }

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] = {
    case Left(a)  => responseA(a)
    case Right(b) => responseB(b)
  }

  def request[A, B, C, AB, Out](
      method: Method,
      url: Url[A],
      entity: RequestEntity[B] = emptyRequest,
      docs: Documentation = None,
      headers: RequestHeaders[C] = emptyRequestHeaders
  )(implicit
      tuplerAB: Tupler.Aux[A, B, AB],
      tuplerABC: Tupler.Aux[AB, C, Out]
  ): Request[Out] = {
    val (m, u, e, h) = (method, url, entity, headers)
    new Request[Out] {
      type UrlData = A
      type HeadersData = C
      type EntityData = B

      def url: Url[A] = u
      def method: Method = m
      def entity: RequestEntity[B] = e
      def headers: RequestHeaders[C] = h

      def urlData(out: Out): UrlData = {
        val (ab, _) = tuplerABC.unapply(out)
        val (a, _) = tuplerAB.unapply(ab)
        a
      }

      private[server] def aggregateAndValidate(
          urlData: UrlData,
          entityData: EntityData,
          headersData: HeadersData
      ): Validated[Out] =
        Valid(tuplerABC(tuplerAB(urlData, entityData), headersData))

      private[server] lazy val matchAndParseHeadersDirective
          : Directive1[Validated[(UrlData, HeadersData)]] =
        matchAndProvideParsedUrlAndHeadersData(method, url, headers)
    }
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = Endpoint(request, response)

  lazy val directive1InvFunctor: PartialInvariantFunctor[Directive1] =
    new PartialInvariantFunctor[Directive1] {

      def xmapPartial[From, To](
          f: Directive1[From],
          map: From => Validated[To],
          contramap: To => From
      ): Directive1[To] =
        f.flatMap(from =>
          map(from) match {
            case Valid(value)     => Directives.provide(value)
            case invalid: Invalid => handleClientErrors(invalid)
          }
        )
    }

  /** This method is called by ''endpoints'' when an exception is thrown during
    * request processing.
    *
    * The provided implementation uses [[serverErrorResponse]] to complete
    * with a response containing the error message.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleServerError(throwable: Throwable): StandardRoute =
    StandardRoute(serverErrorResponse(throwableToServerError(throwable)))

  override def mapEndpointRequest[A, B, C](
      endpoint: Endpoint[A, B],
      f: Request[A] => Request[C]
  ): Endpoint[C, B] =
    endpoint.copy(request = f(endpoint.request))

  override def mapEndpointResponse[A, B, C](
      endpoint: Endpoint[A, B],
      f: Response[B] => Response[C]
  ): Endpoint[A, C] =
    endpoint.copy(response = f(endpoint.response))

  override def mapEndpointDocs[A, B](
      endpoint: Endpoint[A, B],
      f: EndpointDocs => EndpointDocs
  ): Endpoint[A, B] =
    endpoint

  override def addRequestHeaders[A, H](
      currentRequest: Request[A],
      heads: RequestHeaders[H]
  )(implicit tupler: Tupler[A, H]): Request[tupler.Out] =
    new Request[tupler.Out] {

      type UrlData = currentRequest.UrlData
      type HeadersData = (currentRequest.HeadersData, H)
      type EntityData = currentRequest.EntityData

      def url: Url[UrlData] = currentRequest.url
      def urlData(a: tupler.Out): UrlData = currentRequest.urlData(tupler.unapply(a)._1)
      def method: Method = currentRequest.method
      def entity: RequestEntity[EntityData] = currentRequest.entity

      def headers: RequestHeaders[HeadersData] =
        (httpRequest: HttpRequest) =>
          currentRequest.headers.decode(httpRequest).zip(heads.decode(httpRequest))

      def aggregateAndValidate(
          urlData: UrlData,
          entityData: EntityData,
          headersData: HeadersData
      ): Validated[tupler.Out] =
        currentRequest
          .aggregateAndValidate(urlData, entityData, headersData._1)
          .map(tupler(_, headersData._2))

      private[server] lazy val matchAndParseHeadersDirective
          : Directive1[Validated[(UrlData, HeadersData)]] =
        matchAndProvideParsedUrlAndHeadersData(method, url, headers)
    }

  override def addRequestQueryString[A, Q](
      request: Request[A],
      qs: QueryString[Q]
  )(implicit tupler: Tupler[A, Q]): Request[tupler.Out] =
    new Request[tupler.Out] {
      type UrlData = (request.UrlData, Q)
      type HeadersData = request.HeadersData
      type EntityData = request.EntityData

      def method: Method = request.method
      def url: Url[UrlData] = request.url.addQueryString(qs)
      def entity: RequestEntity[EntityData] = request.entity
      def headers: RequestHeaders[HeadersData] = request.headers

      def urlData(o: tupler.Out): UrlData = {
        val (a, q) = tupler.unapply(o)
        val urlData = request.urlData(a)
        (urlData, q)
      }

      private[server] def aggregateAndValidate(
          urlData: UrlData,
          entityData: EntityData,
          headersData: HeadersData
      ): Validated[tupler.Out] =
        request.aggregateAndValidate(urlData._1, entityData, headersData).map(tupler(_, urlData._2))

      private[server] lazy val matchAndParseHeadersDirective
          : Directive1[Validated[(UrlData, HeadersData)]] =
        matchAndProvideParsedUrlAndHeadersData(method, url, headers)
    }

  override def addResponseHeaders[A, H](
      response: Response[A],
      headers: ResponseHeaders[H]
  )(implicit tupler: Tupler[A, H]): Response[tupler.Out] =
    o => {
      val (a, h) = tupler.unapply(o)
      val httpHeaders = headers(h)
      val route = response(a)
      requestContext => {
        implicit val ec = requestContext.executionContext
        route(requestContext).map {
          case RouteResult.Complete(response) =>
            RouteResult.Complete(response.withHeaders(response.headers ++ httpHeaders))
          case r: RouteResult.Rejected => r
        }
      }
    }
}
