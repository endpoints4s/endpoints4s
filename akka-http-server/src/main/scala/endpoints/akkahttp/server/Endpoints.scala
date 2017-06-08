package endpoints.akkahttp.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import endpoints.algebra.{Decoder, Encoder, MuxRequest}
import endpoints.{Tupler, algebra}

import scala.concurrent.Future
import scala.language.higherKinds
import scala.util.{Failure, Success}

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using akka-http.
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
      Directives.onComplete(implementation(arguments)) {
        case Success(result) => response(result)
        case Failure(ex) => Directives.complete(ex)
      }
    }

  }

  def emptyRequest: RequestEntity[Unit] = convToDirective1(Directives.pass)

  def emptyHeaders: RequestHeaders[Unit] = convToDirective1(Directives.pass)

  def joinHeaders[H1, H2](h1: RequestHeaders[H1], h2: RequestHeaders[H2])(implicit tupler: Tupler[H1, H2]): RequestHeaders[tupler.Out] = {
    h1.flatMap(h1p => h2.map(h2p => tupler.apply(h1p, h2p)))
  }

  def emptyResponse: Response[Unit] = x => Directives.complete((StatusCodes.OK, ""))

  def textResponse: Response[String] = x => Directives.complete((StatusCodes.OK, x))

  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] = {
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

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] = Endpoint(request, response)


  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](request: Request[Transport], response: Response[Transport]) {

    def implementedBy(handler: MuxHandler[Req, Resp])(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): Route = handleAsync(req => Future.successful(handler(req)))

    def implementedByAsync(handler: MuxHandlerAsync[Req, Resp])(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): Route = handleAsync(req => handler(req))

    private def handleAsync(handler: Req {type Response = Resp} => Future[Resp])(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): Route =
      request { request =>
        Directives.onComplete(handler(decoder.decode(request).right.get /* TODO Handle failure */ .asInstanceOf[Req {type Response = Resp}])) {
          case Success(result) => response(encoder.encode(result))
          case Failure(ex) => Directives.complete(ex)
        }
      }

  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)


}

/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req  Request base type
  * @tparam Resp Response base type
  */
trait MuxHandlerAsync[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req {type Response = R}): Future[R]
}

/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req  Request base type
  * @tparam Resp Response base type
  */
trait MuxHandler[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req {type Response = R}): R
}