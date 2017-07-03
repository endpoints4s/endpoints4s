package endpoints.play.server

import endpoints.algebra
import endpoints.Tupler
import endpoints.algebra.{Decoder, Encoder, MuxRequest}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.InvariantFunctor
import play.api.libs.functional.syntax._
import play.api.libs.streams.Accumulator
import play.api.mvc.{Handler => PlayHandler, _}
import play.twirl.api.Html

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * Interpreter for [[algebra.Endpoints]] that performs routing using Play framework.
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
  */
trait Endpoints extends algebra.Endpoints with Urls with Methods {

  /**
    * An attempt to extract an `A` from a request headers.
    *
    * Models failure by returning a `Left(result)`. That makes it possible
    * to early return an HTTP response if a header is wrong (e.g. if
    * an authentication information is missing)
    */
  type RequestHeaders[A] = Headers => Either[Result, A]

  /** Always succeeds in extracting no information from the headers */
  lazy val emptyHeaders: RequestHeaders[Unit] = _ => Right(())

  /** Succeeds if both informations can be extracted from headers*/
  def joinHeaders[H1, H2](h1: RequestHeaders[H1], h2: RequestHeaders[H2])(implicit tupler: Tupler[H1, H2]): RequestHeaders[tupler.Out] = {
    headers => {
      h1(headers).right.flatMap(h1p => h2(headers).right.map(h2p => tupler.apply(h1p, h2p)))
    }
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
      * Two kinds of failures can happen:
      * 1. The incoming request URL does not match `this` definition: nothing
      *    is extracted (the `RequestExtractor` returns `None`) ;
      * 2. The incoming request URL matches `this` definition but the headers
      *    are erroneous: the `RequestExtractor` returns a `Left(result)`.
      */
    def decode: RequestExtractor[Either[Result, A]]

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
              case Left(result) => BodyParser(_ => Accumulator.done(Left(result)))
              case Right(a) => toB(a)
            }
        def encode(b: B): Call = parent.encode(toA(b))
      }
  }

  /** Decodes a request entity */
  type RequestEntity[A] = BodyParser[A]

  lazy val emptyRequest: BodyParser[Unit] = BodyParser(_ => Accumulator.done(Right(())))


  private def extractMethodUrlAndHeaders[A, B](method: Method, url: Url[A], headers: RequestHeaders[B]): UrlAndHeaders[(A, B)] =
    new UrlAndHeaders[(A, B)] {
      val decode: RequestExtractor[Either[Result, (A, B)]] =
        request =>
          (method.extract: RequestExtractor[Unit])
            .andKeep(url.decodeUrl)
            .apply(request)
            .map { a => headers(request.headers).right.map((a, _))
          }
      def encode(ab: (A, B)): Call = Call(method.value, url.encodeUrl(ab._1))
    }

  /**
    * Decodes a request that uses the POST HTTP verb.
    * @param url Request URL
    * @param entity Request entity
    * @param headers Request headers
    */
  def request[A, B, C, AB](method: Method, url: Url[A], entity: RequestEntity[B], headers: RequestHeaders[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
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

  /** A successful HTTP response (status code 200) with no entity */
  lazy val emptyResponse: Response[Unit] = _ => Results.Ok

  /** A successful HTTP response (status code 200) with string entity */
  lazy val textResponse: Response[String] = x => Results.Ok(x)

  /** A successful HTTP response (status code 200) with an HTML entity */
  lazy val htmlResponse: Response[Html] = html => Results.Ok(html)

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
      endpoint.request.decode(header)
        .map { bodyParser =>
          Action.async(bodyParser) { request =>
            service(request.body).map { b =>
              endpoint.response(b)
            }
          }
        }
  }

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    Endpoint(request, response)

  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ) {
    def implementedBy(
      handler: MuxHandler[Req, Resp]
    )(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): ToPlayHandler =
      toPlayHandler(req => Future.successful(handler(req)))

    def implementedByAsync(
      handler: MuxHandlerAsync[Req, Resp]
    )(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): ToPlayHandler =
      toPlayHandler(req => handler(req))

    def toPlayHandler(
      handler: Req { type Response = Resp } => Future[Resp]
    )(implicit
      decoder: Decoder[Transport, Req],
      encoder: Encoder[Resp, Transport]
    ): ToPlayHandler =
      header =>
        request.decode(header).map { bodyParser =>
          Action.async(bodyParser) { request =>
            handler(decoder.decode(request.body).right.get /* TODO Handle failure */.asInstanceOf[Req { type Response = Resp}])
              .map(resp => response(encoder.encode(resp)))
          }
        }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Transport => Result
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

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

}

//#mux-handler-async
/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req Request base type
  * @tparam Resp Response base type
  */
trait MuxHandlerAsync[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): Future[R]
}
//#mux-handler-async

/**
  * A function whose return type depends on the type
  * of the given `req`.
  *
  * @tparam Req Request base type
  * @tparam Resp Response base type
  */
trait MuxHandler[Req <: MuxRequest, Resp] {
  def apply[R <: Resp](req: Req { type Response = R }): R
}