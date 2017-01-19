package endpoints.akkahttp

import akka.http.javadsl.server.RouteResult
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import endpoints.{Tupler, algebra}

import scala.language.higherKinds

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Play framework.
  *
  *
  */
class Endpoints extends algebra.Endpoints with Urls with Methods {

  type RequestHeaders[A] = Directive1[A]

  type Request[A] = Directive1[A]

  type RequestEntity[A] = Directive1[A]

  type Response[A] = A => Route

  type Endpoint[A, B] = (A => B) => Route

  override def emptyRequest: RequestEntity[Unit] = convToDirective1(Directives.pass)

  override def emptyHeaders: RequestHeaders[Unit] = convToDirective1(Directives.pass)

  override def emptyResponse: Response[Unit] = x => Directives.complete((StatusCodes.OK, ""))

  override def request[A, B, C, AB](
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

  override def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    (implementation: A => B) => request { arguments =>
      response(implementation(arguments))
    }

}
