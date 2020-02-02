package endpoints.play.server

import endpoints.algebra.Documentation
import endpoints.{Invalid, Semigroupal, Tupler, Valid, Validated, algebra}
import play.api.http.{HttpEntity, Writeable}
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
trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Play framework.
  * @group interpreters
  */
trait EndpointsWithCustomErrors extends algebra.EndpointsWithCustomErrors with Urls with Methods with StatusCodes {

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
  lazy val emptyHeaders: RequestHeaders[Unit] = _ => Valid(())

  def header(name: String,docs: Option[String]): Headers => Validated[String] =
    headers => headers.get(name) match {
      case Some(value) => Valid(value)
      case None        => Invalid(s"Missing header $name")
    }

  def optHeader(name: String,docs: Option[String]): Headers => Validated[Option[String]] =
    headers => Valid(headers.get(name))

  implicit lazy val reqHeadersInvFunctor: endpoints.InvariantFunctor[RequestHeaders] = new endpoints.InvariantFunctor[RequestHeaders] {
    def xmap[A, B](fa: RequestHeaders[A], f: A => B, g: B => A): RequestHeaders[B] =
      headers => fa(headers).map(f)
  }

  implicit lazy val reqHeadersSemigroupal: Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders] {
    def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit tupler: Tupler[A, B]): RequestHeaders[tupler.Out] =
      headers => fa(headers).zip(fb(headers))
  }

  /**
    * An HTTP request.
    *
    * Has an instance of `InvariantFunctor`.
    */
  trait Request[A] {
    /**
      * Extracts a `BodyParser[A]` from an incoming request. That is
      * a way to extract an `A` from an incoming request.
      */
    def decode: RequestExtractor[BodyParser[A]]

    /**
      * Reverse routing.
      * @param a Information carried by the request
      * @return The URL and HTTP verb matching the `a` value.
      */
    def encode(a: A): Call
  }

  implicit lazy val invariantFunctorRequest: InvariantFunctor[Request] =
    new InvariantFunctor[Request] {
      def inmap[A, B](m: Request[A], f1: A => B, f2: B => A): Request[B] =
        new Request[B] {
          def decode: RequestExtractor[BodyParser[B]] =
            functorRequestExtractor.fmap(m.decode, (bodyParser: BodyParser[A]) => bodyParser.map(f1))
          def encode(a: B): Call = m.encode(f2(a))
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
    def toRequest[B](toB: A => BodyParser[B])(toA: B => A): Request[B] =
      new Request[B] {
        def decode: RequestExtractor[BodyParser[B]] =
          request =>
            parent.decode(request).map {
              case inv: Invalid => BodyParser(_ => Accumulator.done(Left(handleClientErrors(inv))))
              case Valid(a) => toB(a)
            }
        def encode(b: B): Call = parent.encode(toA(b))
      }
  }

  /** Decodes a request entity */
  type RequestEntity[A] = BodyParser[A]

  lazy val emptyRequest: BodyParser[Unit] = BodyParser(_ => Accumulator.done(Right(())))

  lazy val textRequest: BodyParser[String] = playComponents.playBodyParsers.text

  implicit def reqEntityInvFunctor: endpoints.InvariantFunctor[RequestEntity] = new endpoints.InvariantFunctor[RequestEntity] {
    def xmap[From, To](f: BodyParser[From], map: From => To, contramap: To => From): BodyParser[To] =
      f.map(map)
  }

  protected def extractMethodUrlAndHeaders[A, B](method: Method, url: Url[A], headers: RequestHeaders[B]): UrlAndHeaders[(A, B)] =
    new UrlAndHeaders[(A, B)] {
      val decode: RequestExtractor[Validated[(A, B)]] =
        request => method.extract(request).flatMap { _ =>
          url.decodeUrl(request).map { validatedA =>
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
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    extractMethodUrlAndHeaders(method, url, headers)
      .toRequest {
        case (a, c) => entity.map(b => tuplerABC.apply(tuplerAB.apply(a, b), c))
      } { abc =>
        val (ab, c) = tuplerABC.unapply(abc)
        val (a, _) = tuplerAB.unapply(ab)
        (a, c)
      }


  /**
    * Turns the `A` information into a proper Play `Result`
    */
  type Response[A] = A => Result

  implicit lazy val responseInvFunctor: endpoints.InvariantFunctor[Response] =
    new endpoints.InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] = fa compose g
    }

  type ResponseEntity[A] = A => HttpEntity

  private[server] def responseEntityFromWriteable[A](writeable: Writeable[A]): ResponseEntity[A] =
    a => writeable.toEntity(a)

  /** An empty response entity */
  def emptyResponse: ResponseEntity[Unit] =
    responseEntityFromWriteable(Writeable.writeableOf_EmptyContent.map[Unit](_ => Results.EmptyContent()))

  /** A text entity */
  def textResponse: ResponseEntity[String] = responseEntityFromWriteable(implicitly)

  /** A successful HTTP response (status code 200) with an HTML entity */
  lazy val htmlResponse: ResponseEntity[Html] = responseEntityFromWriteable(implicitly)

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): A => Result =
    a => statusCode.sendEntity(entity(a))

  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]] = {
    case Left(a)  => responseA(a)
    case Right(b) => responseB(b)
  }

  /**
    * @return An HTTP response redirecting to another endpoint (using 303 code status).
    * @param other Endpoint to redirect to
    * @param args Arguments to pass to the endpoint to generate its URL
    */
  def redirect[A](other: => Endpoint[A, _])(args: A): Response[Unit] = _ => Results.Redirect(other.call(args))

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
    def implementedBy(service: A => B): EndpointWithHandler[A, B] = EndpointWithHandler(this, service andThen Future.successful)

    /**
      * Same as `implementedBy`, but with an async `service`.
      */
    def implementedByAsync(service: A => Future[B]): EndpointWithHandler[A, B] = EndpointWithHandler(this, service)
  }

  /**
    * An endpoint from which we can get a Play request handler.
    */
  case class EndpointWithHandler[A, B](endpoint: Endpoint[A, B], service: A => Future[B]) extends ToPlayHandler {
    /**
      * Builds a request `Handler` (a Play `Action`) if the incoming request headers matches
      * the `endpoint` definition.
      */
    def playHandler(header: RequestHeader): Option[PlayHandler] =
      try {
        endpoint.request.decode(header)
          .map { bodyParser =>
            EssentialAction { headers =>
              try {
                val action =
                  playComponents.defaultActionBuilder.async(bodyParser) { request =>
                    service(request.body).map { b =>
                      endpoint.response(b)
                    }
                  }
                action(headers).recover {
                  case NonFatal(t) => handleServerError(t)
                }
              } catch {
                case NonFatal(t) => Accumulator.done(handleServerError(t))
              }
            }
          }
      } catch {
        case NonFatal(t) => Some(playComponents.defaultActionBuilder(_ => handleServerError(t)))
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
  def routesFromEndpoints(endpoints: ToPlayHandler*): PartialFunction[RequestHeader, PlayHandler] =
    Function.unlift { request : RequestHeader =>
      def loop(es: Seq[ToPlayHandler]): Option[PlayHandler] =
        es match {
          case e +: es2 => e.playHandler(request).orElse(loop(es2))
          case Nil => None
        }
      loop(endpoints)
    }

  implicit def EmptyEndpointToPlayHandler[A, B](endpoint: Endpoint[A, B])(implicit ev: Unit =:= B): ToPlayHandler =
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
