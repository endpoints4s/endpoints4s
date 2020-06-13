package endpoints.akkahttp.server

import akka.http.scaladsl.marshalling.{
  Marshaller,
  ToEntityMarshaller,
  ToResponseMarshaller
}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, MediaTypes}
import akka.http.scaladsl.server.{
  Directive1,
  Directives,
  ExceptionHandler,
  Route,
  StandardRoute
}
import akka.http.scaladsl.unmarshalling._
import endpoints.algebra.Documentation
import endpoints.{
  Invalid,
  InvariantFunctor,
  PartialInvariantFunctor,
  Semigroupal,
  Tupler,
  Valid,
  Validated,
  algebra
}

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Akka-HTTP and uses [[algebra.BuiltInErrors]]
  * to model client and server errors.
  *
  * @group interpreters
  */
trait Endpoints
    extends algebra.Endpoints
    with EndpointsWithCustomErrors
    with BuiltInErrors

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Akka-HTTP.
  * @group interpreters
  */
trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  type RequestHeaders[A] = Directive1[A]

  type Request[A] = Directive1[A]

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

  implicit lazy val responseEntityInvariantFunctor
      : InvariantFunctor[ResponseEntity] =
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
    def implementedBy(implementation: A => B): Route =
      Directives.handleExceptions(endpointsExceptionHandler) {
        request { arguments => response(implementation(arguments)) }
      }

    def implementedByAsync(implementation: A => Future[B]): Route =
      Directives.handleExceptions(endpointsExceptionHandler) {
        request { arguments =>
          Directives.onComplete(implementation(arguments)) {
            case Success(result) => response(result)
            case Failure(ex)     => throw ex
          }
        }
      }
  }

  /* ************************
      REQUESTS
  ************************* */

  implicit def requestPartialInvariantFunctor
      : PartialInvariantFunctor[Request] =
    directive1InvFunctor

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

  implicit lazy val requestEntityPartialInvariantFunctor
      : PartialInvariantFunctor[RequestEntity] =
    directive1InvFunctor

  /* ************************
      HEADERS
  ************************* */

  def emptyRequestHeaders: RequestHeaders[Unit] =
    convToDirective1(Directives.pass)

  def requestHeader(name: String, docs: Documentation): RequestHeaders[String] =
    Directives.headerValueByName(name)

  def optRequestHeader(
      name: String,
      docs: Documentation
  ): RequestHeaders[Option[String]] = Directives.optionalHeaderValueByName(name)

  implicit lazy val requestHeadersPartialInvariantFunctor
      : PartialInvariantFunctor[RequestHeaders] =
    directive1InvFunctor
  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      override def product[A, B](fa: Directive1[A], fb: Directive1[B])(
          implicit tupler: Tupler[A, B]
      ): Directive1[tupler.Out] = joinDirectives(fa, fb)
    }

  /* ************************
      RESPONSES
  ************************* */

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
      : InvariantFunctor[ResponseHeaders] =
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
  )(
      implicit
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
  )(
      implicit tuplerAB: Tupler.Aux[A, B, AB],
      tuplerABC: Tupler.Aux[AB, C, Out]
  ): Request[Out] = {
    val methodDirective = convToDirective1(Directives.method(method))
    val matchDirective = methodDirective & url.directive & headers
    matchDirective.tflatMap {
      case (_, a, c) =>
        entity.map(b => tuplerABC(tuplerAB(a, b), c))
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

  /**
    * This method is called by ''endpoints'' when an exception is thrown during
    * request processing.
    *
    * The provided implementation uses [[serverErrorResponse]] to complete
    * with a response containing the error message.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleServerError(throwable: Throwable): StandardRoute =
    StandardRoute(serverErrorResponse(throwableToServerError(throwable)))

}
