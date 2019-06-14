package endpoints.http4s.server

import cats.effect.Sync
import cats.implicits._
import endpoints.algebra.Documentation
import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}
import fs2._
import org.http4s
import org.http4s.{Charset, Headers, MediaType}

trait Endpoints[F[_]] extends algebra.Endpoints with Methods with Urls[F] {
  implicit def F: Sync[F]

  type RequestHeaders[A] = http4s.Headers => Either[ErrorResponse, A]

  type Request[A] =
    PartialFunction[http4s.Request[F], Either[ErrorResponse, F[A]]]

  type RequestEntity[A] = http4s.Request[F] => F[A]

  type Response[A] = A => http4s.Response[F]

  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def implementedBy(implementation: A => B)
      : PartialFunction[http4s.Request[F], F[http4s.Response[F]]] = {
      case req: http4s.Request[F] if request.isDefinedAt(req) =>
        request(req) match {
          case Right(a)            => a.map(implementation).map(response)
          case Left(errorResponse) => errorResponse.pure[F]
        }
    }

    def implementedByEffect(implementation: A => F[B]): PartialFunction[http4s.Request[F], F[http4s.Response[F]]] = {
      case req: http4s.Request[F] if request.isDefinedAt(req) =>
        request(req) match {
          case Right(a)            => a.flatMap(implementation).map(response)
          case Left(errorResponse) => errorResponse.pure[F]
        }
    }
  }

  /**
    * HEADERS
    */
  def emptyHeaders: RequestHeaders[Unit] = _ => Right(())

  def header(name: String, docs: Documentation): RequestHeaders[String] =
    headers =>
      headers.filter(_.name.value == name).collectFirst {
        case h => h.name.value
      } match {
        case Some(value) => Right(value)
        case None        => Left(badRequestResponse)
    }

  def optHeader(name: String,
                docs: Documentation): RequestHeaders[Option[String]] =
    headers =>
      headers.filter(_.name.value == name).collectFirst {
        case h => Some(h.name.value)
      } match {
        case Some(value) => Right(value)
        case None        => Left(badRequestResponse)
    }

  /**
    * RESPONSES
    */
  def emptyResponse(docs: Documentation): Response[Unit] =
    _ => http4s.Response[F](status = http4s.Status.NoContent)

  def textResponse(docs: Documentation): Response[String] =
    str =>
      http4s
        .Response[F](
          status = http4s.Status.Ok,
          body = fs2.Stream(str).through(text.utf8Encode),
          headers = Headers(
            http4s.headers
              .`Content-Type`(MediaType.text.plain, Charset.`UTF-8`)
              .pure[List])
        )

  def wheneverFound[A](response: Response[A],
                       notFoundDocs: Documentation): Response[Option[A]] = {
    case Some(a) => response(a)
    case None    => http4s.Response.notFound[F]
  }

  def endpoint[A, B](request: Request[A],
                     response: Response[B],
                     summary: Documentation,
                     description: Documentation,
                     tags: List[String]): Endpoint[A, B] =
    Endpoint(request, response)

  /**
    * REQUESTS
    */
  def emptyRequest: RequestEntity[Unit] = _ => ().pure[F]

  def textRequest(docs: Documentation): RequestEntity[String] =
    req => req.body.through(text.utf8Decode).compile.toList.map(_.mkString)

  def request[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      method: Method,
      url: Url[UrlP],
      entity: RequestEntity[BodyP] = emptyRequest,
      headers: RequestHeaders[HeadersP] = emptyHeaders
  )(implicit tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
    tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]): Request[Out] =
    Function.unlift(
      req =>
        if (req.method == method) {
          url
            .decodeUrl(req.uri)
            .map(_.flatMap(u =>
              headers(req.headers).map(h =>
                entity(req).map(body => tuplerUBH(tuplerUB(u, body), h)))))
        } else
        None)

  implicit def reqEntityInvFunctor: endpoints.InvariantFunctor[RequestEntity] =
    new InvariantFunctor[RequestEntity] {
      override def xmap[From, To](
          f: http4s.Request[F] => F[From],
          map: From => To,
          contramap: To => From): http4s.Request[F] => F[To] =
        body => f(body).map(map)
    }

  implicit def reqHeadersInvFunctor
    : endpoints.InvariantFunctor[RequestHeaders] =
    new InvariantFunctor[RequestHeaders] {
      override def xmap[From, To](
          f: Headers => Either[ErrorResponse, From],
          map: From => To,
          contramap: To => From): Headers => Either[ErrorResponse, To] =
        headers => f(headers).map(map)
    }

  implicit def reqHeadersSemigroupal: endpoints.Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      override def product[A, B](fa: Headers => Either[ErrorResponse, A],
                                 fb: Headers => Either[ErrorResponse, B])(
          implicit tupler: Tupler[A, B])
        : Headers => Either[ErrorResponse, tupler.Out] =
        headers =>
          fa(headers)
            .flatMap(a => fb(headers).map(b => tupler(a, b)))
    }
}
