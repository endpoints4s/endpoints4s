package endpoints.akkahttp.server

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.{Directive1, Directives, ExceptionHandler, Route, StandardRoute}
import akka.http.scaladsl.unmarshalling._
import endpoints.algebra.Documentation
import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Akka-HTTP and uses [[algebra.BuiltInErrors]]
  * to model client and server errors.
  *
  * @group interpreters
  */
trait Endpoints extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Akka-HTTP.
  * @group interpreters
  */
trait EndpointsWithCustomErrors extends algebra.EndpointsWithCustomErrors with Urls with Methods with StatusCodes {

  type RequestHeaders[A] = Directive1[A]

  type Request[A] = Directive1[A]

  type RequestEntity[A] = Directive1[A]

  type ResponseEntity[A] = ToEntityMarshaller[A]

  type Response[A] = A => Route

  implicit lazy val responseInvFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] = fa compose g
    }

  private[server] val endpointsExceptionHandler =
    ExceptionHandler { case NonFatal(t) => handleServerError(t) }

  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def implementedBy(implementation: A => B): Route =
      Directives.handleExceptions(endpointsExceptionHandler) {
        request { arguments =>
          response(implementation(arguments))
        }
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

  def emptyRequest: RequestEntity[Unit] = convToDirective1(Directives.pass)

  def textRequest: RequestEntity[String] = {
    val um: FromRequestUnmarshaller[String] = implicitly
    Directives.entity[String](um)
  }

  implicit lazy val reqEntityInvFunctor: InvariantFunctor[RequestEntity] = directive1InvFunctor

  /* ************************
      HEADERS
  ************************* */

  def emptyHeaders: RequestHeaders[Unit] = convToDirective1(Directives.pass)

  def header(name: String, docs: Documentation): RequestHeaders[String] = Directives.headerValueByName(name)

  def optHeader(name: String, docs: Documentation): RequestHeaders[Option[String]] = Directives.optionalHeaderValueByName(name)

  implicit lazy val reqHeadersInvFunctor: InvariantFunctor[RequestHeaders] = directive1InvFunctor
  implicit lazy val reqHeadersSemigroupal: Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders] {
    override def product[A, B](fa: Directive1[A], fb: Directive1[B])(implicit tupler: Tupler[A, B]): Directive1[tupler.Out] = joinDirectives(fa, fb)
  }

  /* ************************
      RESPONSES
  ************************* */

  def emptyResponse: ResponseEntity[Unit] = Marshaller.opaque(_ => HttpEntity.Empty)

  def textResponse: ResponseEntity[String] = implicitly

  def response[A](statusCode: StatusCode, entity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    a => {
      implicit val marshaller: ToResponseMarshaller[A] =
        Marshaller.fromToEntityMarshaller(statusCode)(entity)
      Directives.complete(a)
    }

  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]] = {
    case Left(a)  => responseA(a)
    case Right(b) => responseB(b)
  }

  def request[A, B, C, AB, Out](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    docs: Documentation = None,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] = {
    val methodDirective = convToDirective1(Directives.method(method))
    val matchDirective = methodDirective & url.directive & headers
    matchDirective.tflatMap { case (_, a, c) =>
      entity.map(b => tuplerABC(tuplerAB(a, b), c))
    }
  }

  def endpoint[A, B](
    request: Request[A],
    response: Response[B],
    docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = Endpoint(request, response)

  lazy val directive1InvFunctor: InvariantFunctor[Directive1] = new InvariantFunctor[Directive1] {
    def xmap[From, To](f: Directive1[From], map: From => To, contramap: To => From): Directive1[To] = f.map(map)
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
