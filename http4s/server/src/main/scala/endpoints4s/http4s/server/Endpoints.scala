package endpoints4s.http4s.server

import cats.effect.Sync
import cats.implicits._
import endpoints4s.algebra.Documentation
import endpoints4s.{
  Invalid,
  PartialInvariantFunctor,
  Semigroupal,
  Tupler,
  Valid,
  Validated,
  algebra
}
import org.http4s
import org.http4s.{EntityEncoder, EntityDecoder, Header, Headers}

import scala.util.control.NonFatal
import org.http4s.util.CaseInsensitiveString

/** Interpreter for [[algebra.Endpoints]] based on http4s. It uses [[algebra.BuiltInErrors]]
  * to model client and server errors.
  *
  * Consider the following endpoint definition:
  *
  * {{{
  *   trait MyEndpoints extends algebra.Endpoints with algebra.JsonEntitiesFromSchemas {
  *     val inc = endpoint(get(path / "inc" /? qs[Int]("x")), ok(jsonResponse[Int]))
  *   }
  * }}}
  *
  * You can get an http4s service for it as follow:
  *
  * {{{
  *   object MyService
  *     extends endpoints.http4s.server.Endpoints[IO]
  *       with endpoints.http4s.server.JsonEntitiesFromSchemas
  *       with MyEndpoints {
  *
  *     val service: org.http4s.HttpRoutes[IO] = HttpRoutes.of(
  *       routesFromEndpoints(
  *         inc.implementedBy(x => x + 1)
  *       )
  *     )
  *   }
  * }}}
  *
  * @tparam F Effect type
  */
abstract class Endpoints[F[_]](implicit F: Sync[F])
    extends algebra.Endpoints
    with EndpointsWithCustomErrors
    with BuiltInErrors {

  final type Effect[A] = F[A]
  final implicit def Effect: Sync[Effect] = F

}

/** Interpreter for [[algebra.EndpointsWithCustomErrors]] based on http4s.
  * @group interpreters
  */
trait EndpointsWithCustomErrors extends algebra.EndpointsWithCustomErrors with Methods with Urls {
  type Effect[A]
  implicit def Effect: Sync[Effect]

  type RequestHeaders[A] = http4s.Headers => Validated[A]

  type Request[A] =
    PartialFunction[http4s.Request[Effect], Effect[
      Either[http4s.Response[Effect], A]
    ]]

  type RequestEntity[A] =
    http4s.Request[Effect] => Effect[Either[http4s.Response[Effect], A]]

  type Response[A] = A => http4s.Response[Effect]

  type ResponseEntity[A] = http4s.EntityEncoder[Effect, A]

  type ResponseHeaders[A] = A => http4s.Headers

  case class Endpoint[A, B](
      request: Request[A],
      response: Response[B],
      operationId: Option[String]
  ) {
    def implementedBy(
        implementation: A => B
    ): PartialFunction[http4s.Request[Effect], Effect[
      http4s.Response[Effect]
    ]] =
      implementedByEffect(implementation(_).pure[Effect])

    def implementedByEffect(
        implementation: A => Effect[B]
    ): PartialFunction[http4s.Request[Effect], Effect[
      http4s.Response[Effect]
    ]] = {
      val handler = { (http4sRequest: http4s.Request[Effect]) =>
        try {
          request.lift
            .apply(http4sRequest)
            .map(_.flatMap {
              case Right(a) =>
                try {
                  implementation(a)
                    .map(response)
                    .recoverWith { case NonFatal(t) =>
                      handleServerError(http4sRequest, t)
                    }
                } catch {
                  case NonFatal(t) => handleServerError(http4sRequest, t)
                }
              case Left(errorResponse) => errorResponse.pure[Effect]
            })
        } catch {
          case NonFatal(t) => Some(handleServerError(http4sRequest, t))
        }
      }
      Function.unlift(handler)
    }
  }

  def routesFromEndpoints(
      endpoints: PartialFunction[http4s.Request[Effect], Effect[
        http4s.Response[Effect]
      ]]*
  ) =
    endpoints.reduceLeft(_ orElse _)

  /** HEADERS
    */
  def emptyRequestHeaders: RequestHeaders[Unit] = _ => Valid(())

  def requestHeader(name: String, docs: Documentation): RequestHeaders[String] =
    headers =>
      headers.get(CaseInsensitiveString(name)).map(_.value) match {
        case Some(value) => Valid(value)
        case None        => Invalid(s"Missing header $name")
      }

  def optRequestHeader(
      name: String,
      docs: Documentation
  ): RequestHeaders[Option[String]] =
    headers => Valid(headers.get(CaseInsensitiveString(name)).map(_.value))

  // RESPONSES
  implicit lazy val responseInvariantFunctor: endpoints4s.InvariantFunctor[Response] =
    new endpoints4s.InvariantFunctor[Response] {
      def xmap[A, B](
          fa: Response[A],
          f: A => B,
          g: B => A
      ): Response[B] =
        fa compose g
    }

  implicit def responseEntityInvariantFunctor: endpoints4s.InvariantFunctor[ResponseEntity] =
    new endpoints4s.InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] =
        fa.contramap(g)
    }

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation,
      headers: ResponseHeaders[B]
  )(implicit tupler: Tupler.Aux[A, B, R]): Response[R] =
    r => {
      val (a, b) = tupler.unapply(r)
      http4s.Response[Effect](
        status = statusCode,
        headers = headers(b) ++ entity.headers,
        body = entity.toEntity(a).body
      )
    }

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] = {
    case Left(a)  => responseA(a)
    case Right(b) => responseB(b)
  }

  def emptyResponse: ResponseEntity[Unit] =
    EntityEncoder.emptyEncoder[Effect, Unit]

  def textResponse: ResponseEntity[String] =
    EntityEncoder.stringEncoder

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

  def emptyResponseHeaders: ResponseHeaders[Unit] =
    _ => Headers.empty

  def responseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[String] =
    value => Headers.of(Header(name, value))

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] = {
    case Some(value) => responseHeader(name, docs)(value)
    case None        => emptyResponseHeaders(())
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    Endpoint(request, response, docs.operationId)

  // REQUESTS
  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        Function.unlift(http4sRequest =>
          fa.lift(http4sRequest)
            .map(_.flatMap {
              case Left(response) => Effect.pure(response.asLeft)
              case Right(a) =>
                f(a) match {
                  case Valid(value)     => Effect.pure(value.asRight)
                  case invalid: Invalid => handleClientErrors(http4sRequest, invalid).map(_.asLeft)
                }
            })
        )
    }

  def emptyRequest: RequestEntity[Unit] = _ => Effect.pure(Right(()))

  /* Setting `strict = true` means that this won't accept requests that are
   * missing their Content-Type header. However, if we use `strict = false`,
   * requests with incorrect specified `Content-Type` still get accepted.
   */
  def textRequest: RequestEntity[String] =
    req =>
      EntityDecoder
        .decodeBy(http4s.MediaType.text.plain) { (msg: http4s.Media[Effect]) =>
          http4s.DecodeResult.success(EntityDecoder.decodeText(msg))
        }
        .decode(req, strict = true)
        .leftWiden[Throwable]
        .rethrowT
        .map(Right(_))

  def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    req => {
      def decodedA = requestEntityA(req).map(_.map(Left(_): Either[A, B]))
      def decodedB = requestEntityB(req).map(_.map(Right(_): Either[A, B]))
      decodedA.orElse(decodedB)
    }

  def request[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      method: Method,
      url: Url[UrlP],
      entity: RequestEntity[BodyP] = emptyRequest,
      docs: Documentation = None,
      headers: RequestHeaders[HeadersP] = emptyRequestHeaders
  )(implicit
      tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
      tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]
  ): Request[Out] =
    extractUrlAndHeaders(method, url, headers) { case (u, h) =>
      http4sRequest =>
        entity(http4sRequest).map(
          _.map(body => tuplerUBH(tuplerUB(u, body), h))
        )
    }

  private[server] def extractUrlAndHeaders[U, H, E](
      method: Method,
      url: Url[U],
      headers: RequestHeaders[H]
  )(
      entity: ((U, H)) => RequestEntity[E]
  ): Request[E] =
    Function.unlift { http4sRequest =>
      if (http4sRequest.method == method) {
        url
          .decodeUrl(http4sRequest.uri)
          .map[Validated[(U, H)]](_.zip(headers(http4sRequest.headers)))
          .map {
            case Valid(urlAndHeaders) => entity(urlAndHeaders)(http4sRequest)
            case inv: Invalid         => handleClientErrors(http4sRequest, inv).map(_.asLeft)
          }
      } else None
    }

  implicit def requestEntityPartialInvariantFunctor
      : endpoints4s.PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      def xmapPartial[From, To](
          f: RequestEntity[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestEntity[To] =
        body =>
          f(body).flatMap {
            case Left(response) => Effect.pure(response.asLeft[To])
            case Right(from) =>
              map(from) match {
                case Valid(response)  => Effect.pure(response.asRight)
                case invalid: Invalid => handleClientErrors(body, invalid).map(_.asLeft)
              }
          }

    }

  implicit def requestHeadersPartialInvariantFunctor
      : endpoints4s.PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {
      def xmapPartial[From, To](
          f: RequestHeaders[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestHeaders[To] =
        headers => f(headers).flatMap(map)
    }

  implicit def requestHeadersSemigroupal: endpoints4s.Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): RequestHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  /** This method is called by ''endpoints'' when decoding a request failed.
    *
    * The provided implementation calls `clientErrorsResponse` to construct
    * a response containing the errors.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleClientErrors(
      request: http4s.Request[Effect],
      invalid: Invalid
  ): Effect[http4s.Response[Effect]] =
    Effect.pure(clientErrorsResponse(invalidToClientErrors(invalid)))

  /** This method is called by ''endpoints'' when an exception is thrown during
    * request processing.
    *
    * The provided implementation calls [[serverErrorResponse]] to construct
    * a response containing the error message.
    *
    * This method can be overridden to customize the error reporting logic.
    */
  def handleServerError(
      request: http4s.Request[Effect],
      throwable: Throwable
  ): Effect[http4s.Response[Effect]] =
    Effect.pure(serverErrorResponse(throwableToServerError(throwable)))

}
