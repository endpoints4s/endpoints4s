package endpoints4s.play.server

import endpoints4s.algebra.Documentation
import play.api.http.{HttpEntity, Writeable}
import endpoints4s.{
  Invalid,
  PartialInvariantFunctor,
  Semigroupal,
  Tupler,
  Valid,
  Validated,
  algebra
}
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.libs.functional.InvariantFunctor
import play.api.libs.streams.Accumulator
import play.api.mvc.{Handler => PlayHandler, _}
import play.twirl.api.Html

import scala.concurrent.Future
import scala.util.control.NonFatal

/** Interpreter for [[algebra.Endpoints]] that performs routing using Play framework, and uses
  * [[algebra.BuiltInErrors]] to model client and server errors.
  *
  * Consider the following endpoints definition:
  *
  * {{{
  *   trait MyEndpoints extends algebra.Endpoints with algebra.JsonEntities {
  *     val inc = endpoint(get(path / "inc" ? qs[Int]("x")), jsonResponse[Int])
  *   }
  * }}}
  *
  * You can get a router for them as follows:
  *
  * {{{
  *   object MyRouter extends MyEndpoints with play.server.Endpoints with play.server.JsonEntities {
  *
  *     val routes = routesFromEndpoints(
  *       inc.implementedBy(x => x + 1)
  *     )
  *
  *   }
  * }}}
  *
  * Then `MyRouter.routes` can be used to define a proper Play router as follows:
  *
  * {{{
  *   val router = play.api.routing.Router.from(MyRouter.routes)
  * }}}
  *
  * @group interpreters
  */
trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

/** Interpreter for [[algebra.Endpoints]] that performs routing using Play framework.
  * @group interpreters
  */
trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  val playComponents: PlayComponents

  import playComponents.executionContext

  /** An attempt to extract an `A` from a request headers.
    *
    * Models failure by returning a `Left(result)`. That makes it possible
    * to early return an HTTP response if a header is wrong (e.g. if
    * an authentication information is missing)
    */
  type RequestHeaders[A] = Headers => Validated[A]

  /** Always succeeds in extracting no information from the headers */
  lazy val emptyRequestHeaders: RequestHeaders[Unit] = _ => Valid(())

  def requestHeader(
      name: String,
      docs: Option[String]
  ): Headers => Validated[String] =
    headers =>
      headers.get(name) match {
        case Some(value) => Valid(value)
        case None        => Invalid(s"Missing header $name")
      }

  def optRequestHeader(
      name: String,
      docs: Option[String]
  ): Headers => Validated[Option[String]] =
    headers => Valid(headers.get(name))

  implicit lazy val requestHeadersPartialInvariantFunctor
      : endpoints4s.PartialInvariantFunctor[RequestHeaders] =
    new endpoints4s.PartialInvariantFunctor[RequestHeaders] {

      def xmapPartial[A, B](
          fa: RequestHeaders[A],
          f: A => Validated[B],
          g: B => A
      ): RequestHeaders[B] =
        headers => fa(headers).flatMap(f)
    }

  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): RequestHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  /** An HTTP request.
    *
    * Has an instance of `InvariantFunctor`.
    */
  trait Request[A] {
    type UrlData
    type HeadersData
    type EntityData

    /** The URL component of this request */
    def url: Url[UrlData]

    /** The headers component of this request */
    def headers: RequestHeaders[HeadersData]

    /** The method of this request */
    def method: Method

    /** The entity of this request */
    def entity: RequestEntity[EntityData]
    def aggregateAndValidate(
        urlData: UrlData,
        headersData: HeadersData,
        entityData: EntityData
    ): Validated[A]
    def urlData(a: A): UrlData

    /** Returns a `RequestEntity[A]` (that is, a way to decode the entity of the incoming
      * request) if the incoming request matches the method and URL of this request
      * description.
      */
    def matchRequest(requestHeader: RequestHeader): Option[RequestEntity[A]]

    /** Reverse routing.
      * @param a Information carried by the request
      * @return The URL and HTTP verb matching the `a` value.
      */
    final def encode(a: A): Call = Call(method.value, url.encodeUrl(urlData(a)))

    /** @return If the incoming `requestHeader` match this request description (the
      *         method and URL), validate the request URL parameters and headers.
      *         Otherwise, returns `None`.
      */
    protected final def matchRequestAndParseHeaders(
        requestHeader: RequestHeader
    )(entity: (UrlData, HeadersData) => RequestEntity[A]): Option[RequestEntity[A]] = {
      // Check that the incoming request matches the method and URL of this request description
      val maybeValidatedUrlData =
        (if (method.matches(requestHeader)) Some(()) else None)
          .zip(url.decodeUrl(requestHeader))
          .headOption

      // If the incoming request matches the method and URL of this request description, parse the headers
      val maybeValidatedUrlAndHeadersData: Option[Validated[(UrlData, HeadersData)]] =
        maybeValidatedUrlData.map { case (_, validatedUrlData) =>
          validatedUrlData.zip(headers(requestHeader.headers))
        }

      maybeValidatedUrlAndHeadersData.map {
        case inv: Invalid                  => requestEntityOf(Left(handleClientErrors(inv)))
        case Valid((urlData, headersData)) => entity(urlData, headersData)
      }
    }
  }

  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          request: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        new Request[B] {
          type UrlData = request.UrlData
          type HeadersData = request.HeadersData
          type EntityData = request.EntityData
          def url: Url[UrlData] = request.url
          def headers: RequestHeaders[HeadersData] = request.headers
          def method: Method = request.method
          def entity: RequestEntity[EntityData] = request.entity
          def aggregateAndValidate(
              urlData: UrlData,
              headersData: HeadersData,
              entityData: EntityData
          ): Validated[B] =
            request.aggregateAndValidate(urlData, headersData, entityData).flatMap(f)
          def matchRequest(requestHeader: RequestHeader): Option[RequestEntity[B]] =
            request.matchRequest(requestHeader).map { requestEntityA =>
              requestEntityMapPartial(requestEntityA)(f)
            }
          def urlData(b: B): UrlData = request.urlData(g(b))
        }
    }

  implicit lazy val invariantFunctorRequest: InvariantFunctor[Request] =
    new InvariantFunctor[Request] {
      def inmap[A, B](m: Request[A], f1: A => B, f2: B => A): Request[B] =
        requestPartialInvariantFunctor.xmap(m, f1, f2)
    }

  /** Decodes a request entity */
  type RequestEntity[A] = RequestHeader => Option[BodyParser[A]]

  private[server] def requestEntityOf[A](resultOrValue: Either[Result, A]): RequestEntity[A] =
    _ => Some(BodyParser(_ => Accumulator.done(resultOrValue)))

  lazy val emptyRequest: RequestEntity[Unit] =
    requestEntityOf(Right(()))

  lazy val textRequest: RequestEntity[String] =
    headers => {
      if (headers.contentType.exists(_.equalsIgnoreCase("text/plain"))) {
        Some(playComponents.playBodyParsers.tolerantText)
      } else {
        None
      }
    }

  def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    headers => {
      val maybeBodyParserA = requestEntityA(headers).map(_.map(Left(_)))
      val maybeBodyBarserB = requestEntityB(headers).map(_.map(Right(_)))
      maybeBodyParserA.orElse(maybeBodyBarserB)
    }

  implicit def requestEntityPartialInvariantFunctor: PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
          f: RequestEntity[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestEntity[To] = requestEntityMapPartial(f)(map)
      override def xmap[A, B](fa: RequestEntity[A], f: A => B, g: B => A): RequestEntity[B] =
        requestEntityMap(fa)(f)
    }

  private[server] def requestEntityMapPartial[A, B](
      requestEntity: RequestEntity[A]
  )(f: A => Validated[B]): RequestEntity[B] =
    headers =>
      requestEntity(headers).map(
        _.validate(a =>
          f(a) match {
            case Valid(value)     => Right(value)
            case invalid: Invalid => Left(handleClientErrors(invalid))
          }
        )
      )

  private[server] def requestEntityMap[A, B](requestEntity: RequestEntity[A])(
      f: A => B
  ): RequestEntity[B] =
    headers => requestEntity(headers).map(_.map(f))

  /** Decodes a request.
    * @param url Request URL
    * @param entity Request entity
    * @param docs Request documentation
    * @param headers Request headers
    */
  def request[A, B, C, AB, Out](
      method: Method,
      url: Url[A],
      entity: RequestEntity[B],
      docs: Documentation,
      headers: RequestHeaders[C]
  )(implicit
      tuplerAB: Tupler.Aux[A, B, AB],
      tuplerABC: Tupler.Aux[AB, C, Out]
  ): Request[Out] = {
    val u = url
    val h = headers
    val m = method
    val e = entity
    new Request[Out] {
      type UrlData = A
      type EntityData = B
      type HeadersData = C
      def url: Url[A] = u
      def headers: RequestHeaders[C] = h
      def method: Method = m
      def entity: RequestEntity[B] = e
      def aggregateAndValidate(
          urlData: UrlData,
          headersData: HeadersData,
          entityData: EntityData
      ): Validated[Out] =
        Valid(tuplerABC.apply(tuplerAB.apply(urlData, entityData), headersData))
      def matchRequest(requestHeader: RequestHeader): Option[RequestEntity[Out]] = {
        matchRequestAndParseHeaders(requestHeader) { (urlData, headersData) =>
          requestEntityMap(entity) { entityData =>
            tuplerABC.apply(tuplerAB.apply(urlData, entityData), headersData)
          }
        }
      }
      def urlData(abc: Out): A = {
        val (ab, c) = tuplerABC.unapply(abc)
        val (a, _) = tuplerAB.unapply(ab)
        a
      }
    }
  }

  override def addRequestHeaders[A, H](
      request: Request[A],
      headersP: RequestHeaders[H]
  )(implicit tupler: Tupler[A, H]): Request[tupler.Out] = new Request[tupler.Out] {
    type UrlData = request.UrlData
    type HeadersData = (request.HeadersData, H)
    type EntityData = request.EntityData
    def url: Url[UrlData] = request.url
    def headers: RequestHeaders[HeadersData] =
      hs => request.headers(hs).zip(headersP(hs))
    def method: Method = request.method
    def entity: RequestEntity[EntityData] = request.entity
    def aggregateAndValidate(
        urlData: UrlData,
        headersData: HeadersData,
        entityData: EntityData
    ): Validated[tupler.Out] =
      request
        .aggregateAndValidate(urlData, headersData._1, entityData)
        .map(tupler.apply(_, headersData._2))
    def matchRequest(requestHeader: RequestHeader): Option[RequestEntity[tupler.Out]] = {
      matchRequestAndParseHeaders(requestHeader) { (urlData, headersData) =>
        requestEntityMapPartial(entity) { entityData =>
          aggregateAndValidate(urlData, headersData, entityData)
        }
      }
    }
    def urlData(out: tupler.Out): UrlData =
      request.urlData(tupler.unapply(out)._1)
  }

  override def addRequestQueryString[A, Q](
      request: Request[A],
      qs: QueryString[Q]
  )(implicit tupler: Tupler[A, Q]): Request[tupler.Out] =
    new Request[tupler.Out] {
      type UrlData = (request.UrlData, Q)
      type HeadersData = request.HeadersData
      type EntityData = request.EntityData
      def url: Url[UrlData] = new Url[UrlData] {
        def decodeUrl(req: RequestHeader): Option[Validated[(request.UrlData, Q)]] =
          request.url.decodeUrl(req).map(_.zip(qs.decode(req.queryString)))
        def encodeUrlComponents(
            a: (request.UrlData, Q)
        ): (Seq[String], Map[String, Seq[String]]) = {
          val (path, query) = request.url.encodeUrlComponents(a._1)
          (path, query ++ qs.encode(a._2))
        }

      }
      def headers: RequestHeaders[HeadersData] = request.headers
      def method: Method = request.method
      def entity: RequestEntity[EntityData] = request.entity
      def aggregateAndValidate(
          urlData: UrlData,
          headersData: HeadersData,
          entityData: EntityData
      ): Validated[tupler.Out] =
        request
          .aggregateAndValidate(urlData._1, headersData, entityData)
          .map(tupler.apply(_, urlData._2))
      def matchRequest(requestHeader: RequestHeader): Option[RequestEntity[tupler.Out]] = {
        matchRequestAndParseHeaders(requestHeader) { (urlData, headersData) =>
          requestEntityMapPartial(entity) { entityData =>
            aggregateAndValidate(urlData, headersData, entityData)
          }
        }
      }
      def urlData(out: tupler.Out): UrlData =
        (request.urlData(tupler.unapply(out)._1), tupler.unapply(out)._2)
    }

  override def addResponseHeaders[A, H](
      response: Response[A],
      headers: ResponseHeaders[H]
  )(implicit tupler: Tupler[A, H]): Response[tupler.Out] =
    o => {
      val (a, h) = tupler.unapply(o)
      response(a).withHeaders(headers(h): _*)
    }

  /** Turns the `A` information into a proper Play `Result`
    */
  type Response[A] = A => Result

  implicit lazy val responseInvariantFunctor: endpoints4s.InvariantFunctor[Response] =
    new endpoints4s.InvariantFunctor[Response] {
      def xmap[A, B](
          fa: Response[A],
          f: A => B,
          g: B => A
      ): Response[B] =
        fa compose g
    }

  type ResponseEntity[A] = A => HttpEntity

  implicit lazy val responseEntityInvariantFunctor: endpoints4s.InvariantFunctor[ResponseEntity] =
    new endpoints4s.InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] =
        fa compose g
    }

  private[server] def responseEntityFromWriteable[A](
      writeable: Writeable[A]
  ): ResponseEntity[A] =
    a => writeable.toEntity(a)

  /** An empty response entity */
  def emptyResponse: ResponseEntity[Unit] =
    responseEntityFromWriteable(
      Writeable.writeableOf_EmptyContent.map[Unit](_ => Results.EmptyContent())
    )

  /** A text entity */
  def textResponse: ResponseEntity[String] =
    responseEntityFromWriteable(implicitly)

  /** A successful HTTP response (status code 200) with an HTML entity */
  lazy val htmlResponse: ResponseEntity[Html] = responseEntityFromWriteable(
    implicitly
  )

  type ResponseHeaders[A] = A => Seq[(String, String)]

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

  implicit def responseHeadersInvariantFunctor: endpoints4s.InvariantFunctor[ResponseHeaders] =
    new endpoints4s.InvariantFunctor[ResponseHeaders] {
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
    value => (name, value) :: Nil

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] = {
    case Some(value) => (name, value) :: Nil
    case None        => Nil
  }

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B]
  )(implicit
      tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    r => {
      val (a, b) = tupler.unapply(r)
      val httpHeaders = headers(b)
      statusCode.sendEntity(entity(a)).withHeaders(httpHeaders: _*)
    }

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] = {
    case Left(a)  => responseA(a)
    case Right(b) => responseB(b)
  }

  /** @return An HTTP response redirecting to another endpoint (using 303 code status).
    * @param other Endpoint to redirect to
    * @param args Arguments to pass to the endpoint to generate its URL
    */
  def redirect[A](other: => Endpoint[A, _])(args: A): Response[Unit] =
    _ => Results.Redirect(other.call(args))

  /** Something that can be used as a Play request handler */
  trait ToPlayHandler {
    def playHandler(header: RequestHeader): Option[PlayHandler]
  }

  /** Concrete representation of an `Endpoint` for routing purpose.
    */
  case class Endpoint[A, B](request: Request[A], response: Response[B]) {

    /** Reverse routing */
    def call(a: A): Call = request.encode(a)

    /** Provides an actual implementation to the endpoint definition, to turn it
      * into something effectively usable by the Play router.
      *
      * @param service Function that turns the information carried by the request into
      *                the information necessary to build the response
      */
    def implementedBy(service: A => B): EndpointWithHandler[A, B] =
      EndpointWithHandler(this, service andThen Future.successful)

    /** Same as `implementedBy`, but with an async `service`.
      */
    def implementedByAsync(service: A => Future[B]): EndpointWithHandler[A, B] =
      EndpointWithHandler(this, service)
  }

  /** An endpoint from which we can get a Play request handler.
    */
  case class EndpointWithHandler[A, B](
      endpoint: Endpoint[A, B],
      service: A => Future[B]
  ) extends ToPlayHandler {

    /** Builds a request `Handler` (a Play `Action`) if the incoming request headers matches
      * the `endpoint` definition.
      */
    def playHandler(header: RequestHeader): Option[PlayHandler] =
      try {
        endpoint.request
          .matchRequest(header)
          .map { requestEntity =>
            EssentialAction { headers =>
              try {
                requestEntity(headers) match {
                  case Some(bodyParser) =>
                    val action =
                      playComponents.defaultActionBuilder.async(bodyParser) { request =>
                        service(request.body).map { b =>
                          endpoint.response(b)
                        }
                      }
                    action(headers).recover { case NonFatal(t) =>
                      handleServerError(t)
                    }
                  // Unable to handle request entity
                  case None =>
                    Accumulator.done(
                      playComponents.httpErrorHandler
                        .onClientError(headers, UNSUPPORTED_MEDIA_TYPE)
                    )
                }
              } catch {
                case NonFatal(t) => Accumulator.done(handleServerError(t))
              }
            }
          }
      } catch {
        case NonFatal(t) =>
          Some(playComponents.defaultActionBuilder(_ => handleServerError(t)))
      }
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    Endpoint(request, response)

  override def mapEndpointRequest[A, B, C](
      endpoint: Endpoint[A, B],
      func: Request[A] => Request[C]
  ): Endpoint[C, B] = Endpoint(func(endpoint.request), endpoint.response)

  override def mapEndpointResponse[A, B, C](
      endpoint: Endpoint[A, B],
      func: Response[B] => Response[C]
  ): Endpoint[A, C] = Endpoint(endpoint.request, func(endpoint.response))

  override def mapEndpointDocs[A, B](
      endpoint: Endpoint[A, B],
      func: EndpointDocs => EndpointDocs
  ): Endpoint[A, B] = endpoint

  /** Builds a Play router out of endpoint definitions.
    *
    * {{{
    *   val routes = routesFromEndpoints(
    *     inc.implementedBy(x => x + 1)
    *   )
    * }}}
    */
  def routesFromEndpoints(
      endpoints: ToPlayHandler*
  ): PartialFunction[RequestHeader, PlayHandler] =
    Function.unlift { (request: RequestHeader) =>
      def loop(es: Seq[ToPlayHandler]): Option[PlayHandler] =
        es match {
          case e +: es2 => e.playHandler(request).orElse(loop(es2))
          case Nil      => None
        }
      loop(endpoints)
    }

  implicit def EmptyEndpointToPlayHandler[A, B](
      endpoint: Endpoint[A, B]
  )(implicit ev: Unit =:= B): ToPlayHandler =
    endpoint.implementedBy(_ => ())

  /** This method is called by ''endpoints'' when an exception is thrown during
    * request processing.
    *
    * The provided implementation calls [[serverErrorResponse]] to construct
    * a response containing the error message.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleServerError(throwable: Throwable): Result =
    serverErrorResponse(throwableToServerError(throwable))

}
