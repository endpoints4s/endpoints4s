package endpoints.play.server

import endpoints.algebra.Documentation
import play.api.http.{HttpEntity, Writeable}
import endpoints.{Invalid, PartialInvariantFunctor, Semigroupal, Tupler, Valid, Validated, algebra}
import play.api.http.Status.UNSUPPORTED_MEDIA_TYPE
import play.api.libs.functional.InvariantFunctor
import play.api.libs.streams.Accumulator
import play.api.mvc.{Handler => PlayHandler, _}
import play.twirl.api.Html

import scala.concurrent.Future
import scala.util.control.NonFatal

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Play framework, and uses
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
trait Endpoints
    extends algebra.Endpoints
    with EndpointsWithCustomErrors
    with BuiltInErrors

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Play framework.
  * @group interpreters
  */
trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  val playComponents: PlayComponents

  import playComponents.executionContext

  /**
    * An attempt to extract an `A` from a request headers.
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
      : endpoints.PartialInvariantFunctor[RequestHeaders] =
    new endpoints.PartialInvariantFunctor[RequestHeaders] {
      def xmapPartial[A, B](
          fa: RequestHeaders[A],
          f: A => Validated[B],
          g: B => A
      ): RequestHeaders[B] =
        headers => fa(headers).flatMap(f)
    }

  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(
          implicit tupler: Tupler[A, B]
      ): RequestHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  /**
    * An HTTP request.
    *
    * Has an instance of `InvariantFunctor`.
    */
  trait Request[A] {

    /**
      * Extracts a `RequestEntity[A]` from an incoming request. That is
      * a way to extract an `A` from an incoming request.
      */
    def decode: RequestExtractor[RequestEntity[A]]

    /**
      * Reverse routing.
      * @param a Information carried by the request
      * @return The URL and HTTP verb matching the `a` value.
      */
    def encode(a: A): Call
  }

  implicit def requestPartialInvariantFunctor
      : PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        new Request[B] {
          def decode: RequestExtractor[RequestEntity[B]] =
            functorRequestExtractor.fmap(
              fa.decode,
              (requestEntity: RequestEntity[A]) =>
                requestEntity.xmapPartial(f)(g)
            )
          def encode(b: B): Call = fa.encode(g(b))
        }
    }

  implicit lazy val invariantFunctorRequest: InvariantFunctor[Request] =
    new InvariantFunctor[Request] {
      def inmap[A, B](m: Request[A], f1: A => B, f2: B => A): Request[B] = {
        val transformedRequest = requestPartialInvariantFunctor.xmap(m, f1, f2)
        new Request[B] {
          def decode: RequestExtractor[RequestEntity[B]] =
            transformedRequest.decode
          def encode(b: B): Call = transformedRequest.encode(b)
        }
      }
    }

  /**
    * The URL and HTTP headers of a request.
    */
  trait UrlAndHeaders[A] { parent =>

    /**
      * Attempts to extract an `A` from an incoming request.
      *
      * Two kinds of situations can happen:
      * 1. The incoming request URL does not match `this` definition: nothing
      *    is extracted (the `RequestExtractor` returns `None`) ;
      * 2. The incoming request URL matches `this` definition but the headers or parameters
      *    are erroneous: the `RequestExtractor` returns a `Some(Invalid(...))`.
      */
    def decode: RequestExtractor[Validated[A]]

    /**
      * Reverse routing.
      * @param a Information carried by the request URL and headers
      * @return The URL and HTTP verb matching the `a` value.
      */
    def encode(a: A): Call

    /**
      * Promotes `this` to a `Request[B]`.
      *
      * @param toB Function defining how to get a `BodyParser[B]` from the extracted `A`
      * @param toA Function defining how to get back an `A` from the `B`.
      */
    def toRequest[B](toB: A => RequestEntity[B])(toA: B => A): Request[B] =
      new Request[B] {
        def decode: RequestExtractor[RequestEntity[B]] =
          request =>
            parent.decode(request).map {
              case inv: Invalid =>
                _ =>
                  Some(
                    BodyParser(_ =>
                      Accumulator.done(Left(handleClientErrors(inv)))
                    )
                  )
              case Valid(a) => toB(a)
            }
        def encode(b: B): Call = parent.encode(toA(b))
      }
  }

  /** Decodes a request entity */
  type RequestEntity[A] = RequestHeader => Option[BodyParser[A]]

  lazy val emptyRequest: RequestEntity[Unit] =
    _ => Some(BodyParser(_ => Accumulator.done(Right(()))))

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

  implicit def requestEntityPartialInvariantFunctor
      : PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
          f: RequestEntity[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestEntity[To] =
        headers =>
          f(headers).map(
            _.validate(from =>
              map(from) match {
                case Valid(value)     => Right(value)
                case invalid: Invalid => Left(handleClientErrors(invalid))
              }
            )
          )
    }

  protected def extractMethodUrlAndHeaders[A, B](
      method: Method,
      url: Url[A],
      headers: RequestHeaders[B]
  ): UrlAndHeaders[(A, B)] =
    new UrlAndHeaders[(A, B)] {
      val decode: RequestExtractor[Validated[(A, B)]] =
        request =>
          method.extract(request).flatMap { _ =>
            url.decodeUrl(request).map[Validated[(A, B)]] { validatedA =>
              validatedA.zip(headers(request.headers))
            }
          }
      def encode(ab: (A, B)): Call = Call(method.value, url.encodeUrl(ab._1))
    }

  /**
    * Decodes a request.
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
  )(
      implicit tuplerAB: Tupler.Aux[A, B, AB],
      tuplerABC: Tupler.Aux[AB, C, Out]
  ): Request[Out] =
    extractMethodUrlAndHeaders(method, url, headers)
      .toRequest {
        case (a, c) =>
          headers =>
            entity(headers).map(
              _.map(b => tuplerABC.apply(tuplerAB.apply(a, b), c))
            )
      } { abc =>
        val (ab, c) = tuplerABC.unapply(abc)
        val (a, _) = tuplerAB.unapply(ab)
        (a, c)
      }

  /**
    * Turns the `A` information into a proper Play `Result`
    */
  type Response[A] = A => Result

  implicit lazy val responseInvariantFunctor
      : endpoints.InvariantFunctor[Response] =
    new endpoints.InvariantFunctor[Response] {
      def xmap[A, B](
          fa: Response[A],
          f: A => B,
          g: B => A
      ): Response[B] =
        fa compose g
    }

  type ResponseEntity[A] = A => HttpEntity

  implicit lazy val responseEntityInvariantFunctor
      : endpoints.InvariantFunctor[ResponseEntity] =
    new endpoints.InvariantFunctor[ResponseEntity] {
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
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(
          implicit tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        out => {
          val (a, b) = tupler.unapply(out)
          fa(a) ++ fb(b)
        }
    }

  implicit def responseHeadersInvariantFunctor
      : endpoints.InvariantFunctor[ResponseHeaders] =
    new endpoints.InvariantFunctor[ResponseHeaders] {
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
  )(
      implicit
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

  /**
    * @return An HTTP response redirecting to another endpoint (using 303 code status).
    * @param other Endpoint to redirect to
    * @param args Arguments to pass to the endpoint to generate its URL
    */
  def redirect[A](other: => Endpoint[A, _])(args: A): Response[Unit] =
    _ => Results.Redirect(other.call(args))

  /** Something that can be used as a Play request handler */
  trait ToPlayHandler {
    def playHandler(header: RequestHeader): Option[PlayHandler]
  }

  /**
    * Concrete representation of an `Endpoint` for routing purpose.
    */
  case class Endpoint[A, B](request: Request[A], response: Response[B]) {

    /** Reverse routing */
    def call(a: A): Call = request.encode(a)

    /**
      * Provides an actual implementation to the endpoint definition, to turn it
      * into something effectively usable by the Play router.
      *
      * @param service Function that turns the information carried by the request into
      *                the information necessary to build the response
      */
    def implementedBy(service: A => B): EndpointWithHandler[A, B] =
      EndpointWithHandler(this, service andThen Future.successful)

    /**
      * Same as `implementedBy`, but with an async `service`.
      */
    def implementedByAsync(service: A => Future[B]): EndpointWithHandler[A, B] =
      EndpointWithHandler(this, service)
  }

  /**
    * An endpoint from which we can get a Play request handler.
    */
  case class EndpointWithHandler[A, B](
      endpoint: Endpoint[A, B],
      service: A => Future[B]
  ) extends ToPlayHandler {

    /**
      * Builds a request `Handler` (a Play `Action`) if the incoming request headers matches
      * the `endpoint` definition.
      */
    def playHandler(header: RequestHeader): Option[PlayHandler] =
      try {
        endpoint.request
          .decode(header)
          .map { requestEntity =>
            EssentialAction { headers =>
              try {
                requestEntity(headers) match {
                  case Some(bodyParser) =>
                    val action =
                      playComponents.defaultActionBuilder.async(bodyParser) {
                        request =>
                          service(request.body).map { b => endpoint.response(b) }
                      }
                    action(headers).recover {
                      case NonFatal(t) => handleServerError(t)
                    }
                  // Unable to handle request entity
                  case None =>
                    Accumulator.done(playComponents.httpErrorHandler.onClientError(headers, UNSUPPORTED_MEDIA_TYPE))
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

  /**
    * Builds a Play router out of endpoint definitions.
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

  /**
    * This method is called by ''endpoints'' when an exception is thrown during
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
