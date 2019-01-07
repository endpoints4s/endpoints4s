package endpoints.akkahttp.server

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import endpoints.algebra.Documentation
import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using akka-http.
  *
  * @group interpreters
  */
trait Endpoints extends algebra.Endpoints with Urls with Methods {

  type RequestHeaders[A] = Directive1[A]

  type Request[A] = Directive1[A]

  type RequestEntity[A] = Directive1[A]

  type Response[A] = A => Route

  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def implementedBy(implementation: A => B): Route = request { arguments =>
      response(implementation(arguments))
    }

    def implementedByAsync(implementation: A => Future[B]): Route = request { arguments =>
      Directives.onComplete(implementation(arguments)) {
        case Success(result) => response(result)
        case Failure(ex) => Directives.complete(ex)
      }
    }

  }

  /* ************************
      REQUESTS
  ************************* */

  def emptyRequest: RequestEntity[Unit] = convToDirective1(Directives.pass)

  def textRequest(docs: Documentation): RequestEntity[String] = {
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

  def emptyResponse(docs: Documentation): Response[Unit] = x => Directives.complete((StatusCodes.OK, HttpEntity.Empty))

  def textResponse(docs: Documentation): Response[String] = x => Directives.complete((StatusCodes.OK, x))

  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation): Response[Option[A]] = {
    case Some(a) => response(a)
    case None => Directives.complete(HttpResponse(StatusCodes.NotFound))
  }


  def request[A, B, C, AB, Out](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] = {
    val methodDirective = convToDirective1(Directives.method(method))
    // we use Directives.pathPrefix to construct url directives, so now we close it
    val urlDirective = joinDirectives(url.directive, convToDirective1(Directives.pathEndOrSingleSlash))
    joinDirectives(
      joinDirectives(
        joinDirectives(
          methodDirective,
          urlDirective),
        entity),
      headers)
  }

  def endpoint[A, B](
    request: Request[A],
    response: Response[B],
    summary: Documentation = None,
    description: Documentation = None,
    tags: List[String] = Nil
  ): Endpoint[A, B] = Endpoint(request, response)

  lazy val directive1InvFunctor: InvariantFunctor[Directive1] = new InvariantFunctor[Directive1] {
    override def xmap[From, To](f: Directive1[From], map: From => To, contramap: To => From): Directive1[To] = f.map(map)
  }

}
