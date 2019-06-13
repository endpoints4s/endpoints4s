package endpoints.http4s.server

import cats.effect.Sync
import cats.implicits._
import endpoints.algebra.Documentation
import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}
import fs2._
import org.http4s
import org.http4s.{Charset, Headers, MediaType}

abstract class Endpoints[F[_]](implicit F: Sync[F])
    extends algebra.Endpoints
    with Methods
    with Urls {

  type RequestHeaders[A] = http4s.Headers => Option[A]

  type Request[A] = PartialFunction[http4s.Request[F], F[A]]

  type RequestEntity[A] = http4s.Request[F] => F[A]

  type Response[A] = A => F[http4s.Response[F]]

  case class Endpoint[A, B](request: Request[A], response: Response[B]) {
    def implementedBy(implementation: A => B)
      : PartialFunction[http4s.Request[F], F[http4s.Response[F]]] = {
      case req: http4s.Request[F] if request.isDefinedAt(req) =>
        request(req).map(implementation).flatMap(response)
    }
  }

  /**
    * REQUESTS
    */
  def emptyRequest: RequestEntity[Unit] = _ => F.pure(())

  def textRequest(docs: Documentation): RequestEntity[String] =
    req => req.body.through(text.utf8Decode).compile.toList.map(_.mkString)

  /**
    * HEADERS
    */
  def emptyHeaders: RequestHeaders[Unit] = _ => Some(())

  def header(name: String, docs: Documentation): RequestHeaders[String] =
    headers =>
      headers.filter(_.name.value == name).collectFirst {
        case h => h.name.value
    }

  def optHeader(name: String,
                docs: Documentation): RequestHeaders[Option[String]] =
    headers =>
      headers.filter(_.name.value == name).collectFirst {
        case h => Some(h.name.value)
    }

  /**
    * RESPONSES
    */
  def emptyResponse(docs: Documentation): Response[Unit] =
    _ => F.pure(http4s.Response[F](status = http4s.Status.NoContent))

  def textResponse(docs: Documentation): Response[String] =
    str =>
      F.pure(
        http4s.Response(
          status = http4s.Status.Ok,
          body = fs2.Stream(str).through(text.utf8Encode),
          headers = Headers(
            http4s.headers
              .`Content-Type`(MediaType.text.plain, Charset.`UTF-8`)
              .pure[List])
        )
    )

  def wheneverFound[A](response: Response[A],
                       notFoundDocs: Documentation): Response[Option[A]] = {
    case Some(a) => response(a)
    case None    => F.pure(http4s.Response.notFound)
  }

  def endpoint[A, B](request: Request[A],
                     response: Response[B],
                     summary: Documentation,
                     description: Documentation,
                     tags: List[String]): Endpoint[A, B] =
    Endpoint(request, response)

  def request[UrlP, BodyP, HeadersP, UrlAndBodyPTupled, Out](
      method: Method,
      url: Url[UrlP],
      entity: RequestEntity[BodyP] = emptyRequest,
      headers: RequestHeaders[HeadersP] = emptyHeaders
  )(implicit tuplerUB: Tupler.Aux[UrlP, BodyP, UrlAndBodyPTupled],
    tuplerUBH: Tupler.Aux[UrlAndBodyPTupled, HeadersP, Out]): Request[Out] =
    Function.unlift({ req =>
      for {
        u <- url.decodeUrl(req.uri)
        h <- headers(req.headers)
        _ <- if (req.method == method) Some(()) else None
      } yield
        entity(req).map { body =>
          tuplerUBH(tuplerUB(u, body), h)
        }
    })

  implicit def reqEntityInvFunctor: endpoints.InvariantFunctor[RequestEntity] =
    new InvariantFunctor[RequestEntity] {
      override def xmap[From, To](
          f: http4s.Request[F] => F[From],
          map: From => To,
          contramap: To => From): http4s.Request[F]  => F[To] =
        body => f(body).map(map)
    }

  implicit def reqHeadersInvFunctor
    : endpoints.InvariantFunctor[RequestHeaders] =
    new InvariantFunctor[RequestHeaders] {
      override def xmap[From, To](
          f: Headers => Option[From],
          map: From => To,
          contramap: To => From): Headers => Option[To] =
        headers => f(headers).map(map)
    }

  implicit def reqHeadersSemigroupal: endpoints.Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      override def product[A, B](fa: Headers => Option[A],
                                 fb: Headers => Option[B])(
          implicit tupler: Tupler[A, B]): Headers => Option[tupler.Out] =
        headers =>
          for {
            a <- fa(headers)
            b <- fb(headers)
          } yield tupler(a, b)
    }
}
