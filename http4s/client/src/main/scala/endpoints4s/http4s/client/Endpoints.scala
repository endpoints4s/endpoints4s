package endpoints4s.http4s.client

import cats.effect.Concurrent
import cats.implicits._
import endpoints4s.algebra
import endpoints4s.InvariantFunctor
import endpoints4s.Tupler
import endpoints4s.Semigroupal
import endpoints4s.PartialInvariantFunctor
import endpoints4s.Invalid
import org.http4s.{Request => Http4sRequest, Response => Http4sResponse}
import org.http4s.client.Client
import org.http4s.Headers
import endpoints4s.Validated
import endpoints4s.Valid
import org.typelevel.ci._
import org.http4s.Status
import org.http4s.Uri
import cats.effect.Resource

class Endpoints[F[_]: Concurrent](val host: Uri, val client: Client[F])
    extends algebra.Endpoints
    with EndpointsWithCustomErrors
    with BuiltInErrors {
  type Effect[A] = F[A]
  override def effect: Concurrent[Effect] = Concurrent[F]
}

trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  type Effect[A]
  implicit def effect: Concurrent[Effect]

  def host: Uri
  def client: Client[Effect]

  type RequestHeaders[A] = (A, Http4sRequest[Effect]) => Http4sRequest[Effect]

  override def emptyRequestHeaders: RequestHeaders[Unit] = (_, req) => req

  override def requestHeader(
      name: String,
      docs: Option[String]
  ): RequestHeaders[String] =
    (value, req) => req.putHeaders((name, value))

  override def optRequestHeader(
      name: String,
      docs: Option[String]
  ): RequestHeaders[Option[String]] =
    (value, req) =>
      value match {
        case Some(v) => req.putHeaders((name, v))
        case None    => req
      }

  implicit def requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {

      override def product[A, B](fa: RequestHeaders[A], fb: RequestHeaders[B])(implicit
          tupler: Tupler[A, B]
      ): RequestHeaders[tupler.Out] =
        (out, req) => {
          val (a, b) = tupler.unapply(out)
          fb(b, fa(a, req))
        }

    }

  implicit def requestHeadersPartialInvariantFunctor: PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {

      override def xmapPartial[A, B](
          fa: RequestHeaders[A],
          f: A => Validated[B],
          g: B => A
      ): (B, Http4sRequest[Effect]) => Http4sRequest[Effect] =
        (to, req) => fa(g(to), req)

    }

  type RequestEntity[A] = (A, Http4sRequest[Effect]) => Http4sRequest[Effect]

  implicit def requestEntityPartialInvariantFunctor: PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {

      override def xmapPartial[A, B](
          fa: RequestEntity[A],
          f: A => Validated[B],
          g: B => A
      ): (B, Http4sRequest[Effect]) => Http4sRequest[Effect] =
        (to, req) => fa(g(to), req)

    }

  override def emptyRequest: RequestEntity[Unit] = (_, req) => req

  override def textRequest: RequestEntity[String] =
    (value, req) => req.withEntity(value)

  override def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    (eitherAB, req) => eitherAB.fold(requestEntityA(_, req), requestEntityB(_, req))

  type Request[A] = A => Effect[Http4sRequest[Effect]]

  implicit def requestPartialInvariantFunctor: PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        b => fa(g(b))
    }

  override def request[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      method: Method,
      url: Url[UrlP],
      entity: RequestEntity[BodyP],
      docs: endpoints4s.algebra.Documentation,
      headers: RequestHeaders[HeadersP]
  )(implicit
      tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
      tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]
  ): Request[Out] = { out =>
    val (ub, headersP) = tuplerUBH.unapply(out)
    val (urlP, bodyP) = tuplerUB.unapply(ub)

    effect.map(effect.fromEither(url.encodeUrl(urlP)))(uri =>
      entity(
        bodyP,
        headers(
          headersP,
          Http4sRequest(
            method,
            host.withPath(Uri.Path.unsafeFromString(host.path.renderString + uri.renderString))
          )
        )
      )
    )
  }

  type Response[A] = (StatusCode, Headers) => Option[ResponseEntity[A]]

  type ResponseEntity[A] = Http4sResponse[Effect] => Effect[A]

  implicit def responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {

      override def xmap[A, B](
          fa: (Status, Headers) => Option[Http4sResponse[Effect] => Effect[A]],
          f: A => B,
          g: B => A
      ): (Status, Headers) => Option[Http4sResponse[Effect] => Effect[B]] =
        (sc, hs) => fa(sc, hs).map(_.andThen(_.map(f)))

    }

  implicit def responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] =
        res => fa(res).map(f)
    }

  override def emptyResponse: ResponseEntity[Unit] = _ => ().pure[Effect]

  override def textResponse: ResponseEntity[String] = _.as[String]

  type ResponseHeaders[A] = Headers => Validated[A]

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {

      override def product[A, B](
          fa: Headers => Validated[A],
          fb: Headers => Validated[B]
      )(implicit tupler: Tupler[A, B]): Headers => Validated[tupler.Out] =
        hs => fa(hs).zip(fb(hs))

    }

  implicit def responseHeadersInvariantFunctor: InvariantFunctor[ResponseHeaders] =
    new InvariantFunctor[ResponseHeaders] {

      override def xmap[A, B](
          fa: Headers => Validated[A],
          f: A => B,
          g: B => A
      ): Headers => Validated[B] =
        hs => fa(hs).map(f)

    }

  override def emptyResponseHeaders: ResponseHeaders[Unit] = _ => Valid(())

  override def responseHeader(
      name: String,
      docs: Option[String]
  ): ResponseHeaders[String] =
    hs =>
      hs.get(CIString(name)) match {
        case Some(h) => Valid(h.head.value)
        case None    => Invalid(s"Header '$name' not found")
      }

  override def optResponseHeader(
      name: String,
      docs: Option[String]
  ): ResponseHeaders[Option[String]] =
    hs => Valid(hs.get(CIString(name)).map(_.head.value))

  override def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: algebra.Documentation,
      headers: ResponseHeaders[B]
  )(implicit tupler: Tupler.Aux[A, B, R]): Response[R] =
    (sc, hs) =>
      if (sc != statusCode) None
      else
        headers(hs) match {
          case Invalid(errors) =>
            Some(res => effect.raiseError(new Throwable(errors.mkString(", "))))
          case Valid(b) => Some(res => entity(res).map(tupler.apply(_, b)))
        }

  override def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] =
    (sc, hs) =>
      responseA(sc, hs)
        .map(_.andThen(_.map(_.asLeft[B])))
        .orElse(responseB(sc, hs).map(_.andThen(_.map(_.asRight[A]))))
  //#endpoint-type
  trait Endpoint[A, B] {

    /** This method returns a Resource[Effect, B] unlike `sendAndConsume` this is always safe to use in regard to laziness
      */
    def send(a: A): Resource[Effect, B]

    /** This method might suffer from leaking resource if the handling of the underlying resource is lazy. Use `send` instead in this case.
      */
    def sendAndConsume(request: A): Effect[B] = send(request).use(effect.pure)

    @deprecated(
      since = "6.0.0",
      message =
        "This calls sendAndConsume which is potentially unsafe. Use sendAndConsume if you don't suffer any issues in regard to laziness, else use send"
    )
    def apply(request: A): Effect[B] = sendAndConsume(request)
  }
  //#endpoint-type

  override def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs
  ): Endpoint[A, B] = new Endpoint[A, B] {
    override def send(a: A): Resource[Effect, B] =
      Resource.eval(request(a)).flatMap { req =>
        client
          .run(req)
          .evalMap(res => decodeResponse(response, res).flatMap(_.apply(res)))
      }
  }

  private[client] def decodeResponse[A](
      response: Response[A],
      hResponse: Http4sResponse[Effect]
  ): Effect[ResponseEntity[A]] = {
    val maybeResponse = response(hResponse.status, hResponse.headers)
    def maybeClientErrors =
      clientErrorsResponse(hResponse.status, hResponse.headers)
        .map(
          mapPartialResponseEntity[ClientErrors, A](_)(clientErrors =>
            effect.raiseError(
              new Exception(
                clientErrorsToInvalid(clientErrors).errors.mkString(". ")
              )
            )
          )
        )
    def maybeServerError =
      serverErrorResponse(hResponse.status, hResponse.headers)
        .map(
          mapPartialResponseEntity[ServerError, A](_)(serverError =>
            effect.raiseError(serverErrorToThrowable(serverError))
          )
        )
    effect.fromEither(
      maybeResponse
        .orElse(maybeClientErrors)
        .orElse(maybeServerError)
        .toRight(
          new Throwable(s"Unexpected response status: ${hResponse.status.code}")
        )
    )
  }

  private[client] def mapPartialResponseEntity[A, B](
      entity: ResponseEntity[A]
  )(f: A => Effect[B]): ResponseEntity[B] =
    res => entity(res).flatMap(f)

}
