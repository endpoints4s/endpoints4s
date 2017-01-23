package endpoints.akkahttp.routing

import akka.http.javadsl.server.RouteResult
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import endpoints.{Tupler, algebra}

import scala.concurrent.Future
import scala.language.higherKinds
import scala.util.{Failure, Success}

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Play framework.
  *
  *
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
      Directives.onComplete(implementation(arguments)){
        case Success(result) => response(result)
        case Failure(ex) => Directives.complete(ex)
      }
    }

  }

  def emptyRequest: RequestEntity[Unit] = convToDirective1(Directives.pass)

  def emptyHeaders: RequestHeaders[Unit] = convToDirective1(Directives.pass)

  def emptyResponse: Response[Unit] = x => Directives.complete((StatusCodes.OK, ""))

  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] = {
    val methodDirective = convToDirective1(Directives.method(method))
    joinDirectives(
      joinDirectives(
        joinDirectives(
          methodDirective,
          url.directive),
        entity),
      headers)
  }

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] = Endpoint(request, response)


}
