package endpoints4s.sttp.client

import java.net.URI

import endpoints4s.{
  Invalid,
  InvariantFunctor,
  PartialInvariantFunctor,
  Semigroupal,
  Tupler,
  Valid,
  Validated,
  algebra
}
import endpoints4s.algebra.{Codec, Documentation}
import com.softwaremill.sttp

/**
  * An interpreter for [[endpoints4s.algebra.Endpoints]] that builds a client issuing requests using
  * a sttp’s `com.softwaremill.sttp.SttpBackend`, and uses [[algebra.BuiltInErrors]] to model client
  * and server errors.
  *
  * Doest not support streaming responses for now
  *
  * @param host    Base of the URL of the service that implements the endpoints (e.g. "http://foo.com")
  * @param backend The underlying backend to use
  * @tparam R The monad wrapping the response. It is defined by the backend
  * @group interpreters
  */
class Endpoints[R[_]](
    val host: String,
    val backend: sttp.SttpBackend[R, Nothing]
) extends algebra.Endpoints
    with EndpointsWithCustomErrors[R]
    with BuiltInErrors[R]

/**
  * An interpreter for [[endpoints4s.algebra.Endpoints]] that builds a client issuing requests using
  * a sttp’s `com.softwaremill.sttp.SttpBackend`.
  *
  * @tparam R The monad wrapping the response. It is defined by the backend
  * @group interpreters
  */
trait EndpointsWithCustomErrors[R[_]]
    extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  val host: String
  val backend: sttp.SttpBackend[R, Nothing]

  type SttpRequest = sttp.Request[_, Nothing]

  /**
    * A function that, given an `A` and a request model, returns an updated request
    * containing additional headers
    */
  type RequestHeaders[A] = (A, SttpRequest) => SttpRequest

  /** Does not modify the request */
  lazy val emptyRequestHeaders: RequestHeaders[Unit] = (_, request) => request

  def requestHeader(name: String, docs: Documentation): RequestHeaders[String] =
    (value, request) => request.header(name, value)

  def optRequestHeader(
      name: String,
      docs: Documentation
  ): (Option[String], SttpRequest) => SttpRequest = {
    case (Some(value), request) => request.header(name, value)
    case (None, request)        => request
  }

  implicit lazy val requestHeadersPartialInvariantFunctor
      : PartialInvariantFunctor[RequestHeaders] =
    new PartialInvariantFunctor[RequestHeaders] {
      override def xmapPartial[From, To](
          f: RequestHeaders[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestHeaders[To] =
        (to, request) => f(contramap(to), request)
    }

  implicit lazy val requestHeadersSemigroupal: Semigroupal[RequestHeaders] =
    new Semigroupal[RequestHeaders] {
      override def product[A, B](
          fa: (A, SttpRequest) => SttpRequest,
          fb: (B, SttpRequest) => SttpRequest
      )(
          implicit tupler: Tupler[A, B]
      ): (tupler.Out, SttpRequest) => SttpRequest =
        (ab, request) => {
          val (a, b) = tupler.unapply(ab)
          fa(a, fb(b, request))
        }
    }

  /**
    * A function that takes an `A` information and returns a `sttp.Request`
    */
  type Request[A] = A => SttpRequest

  implicit def requestPartialInvariantFunctor
      : PartialInvariantFunctor[Request] =
    new PartialInvariantFunctor[Request] {
      def xmapPartial[A, B](
          fa: Request[A],
          f: A => Validated[B],
          g: B => A
      ): Request[B] =
        fa compose g
    }

  /**
    * A function that, given an `A` information and a `sttp.Request`, eventually returns a `sttp.Request`
    */
  type RequestEntity[A] = (A, SttpRequest) => SttpRequest

  lazy val emptyRequest: RequestEntity[Unit] = {
    case (_, req) => req
  }

  lazy val textRequest: RequestEntity[String] = {
    case (bodyValue, request) => request.body(bodyValue)
  }

  def choiceRequestEntity[A, B](
      requestEntityA: RequestEntity[A],
      requestEntityB: RequestEntity[B]
  ): RequestEntity[Either[A, B]] =
    (eitherAB, req) =>
      eitherAB.fold(requestEntityA(_, req), requestEntityB(_, req))

  implicit def requestEntityPartialInvariantFunctor
      : PartialInvariantFunctor[RequestEntity] =
    new PartialInvariantFunctor[RequestEntity] {
      override def xmapPartial[From, To](
          f: RequestEntity[From],
          map: From => Validated[To],
          contramap: To => From
      ): RequestEntity[To] =
        (to, req) => f(contramap(to), req)
    }

  def request[A, B, C, AB, Out](
      method: Method,
      url: Url[A],
      entity: RequestEntity[B],
      docs: Documentation,
      headers: RequestHeaders[C]
  )(
      implicit tuplerAB: Tupler.Aux[A, B, AB],
      tuplerABC: Tupler.Aux[AB, C, Out]
  ): Request[Out] =
    (abc: Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)

      val uri: sttp.Id[sttp.Uri] = sttp.Uri(new URI(s"${host}${url.encode(a)}"))
      val sttpRequest: SttpRequest = method(sttp.sttp.get(uri = uri))
      entity(b, headers(c, sttpRequest))
    }

  trait Response[A] {

    /**
      * Function to validate the response (headers, code).
      */
    def decodeResponse(response: sttp.Response[String]): Option[R[A]]
  }

  implicit lazy val responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        new Response[B] {
          def decodeResponse(response: sttp.Response[String]): Option[R[B]] =
            fa.decodeResponse(response)
              .map(ra => backend.responseMonad.map(ra)(f))
        }
    }

  /**
    * Trait that indicates how a response should be interpreted
    */
  trait ResponseEntity[A] {
    def decodeEntity(response: sttp.Response[String]): R[A]
  }

  implicit def responseEntityInvariantFunctor
      : InvariantFunctor[ResponseEntity] =
    new InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] =
        mapResponseEntity(fa)(f)
    }

  private[sttp] def mapResponseEntity[A, B](
      entity: ResponseEntity[A]
  )(f: A => B): ResponseEntity[B] =
    new ResponseEntity[B] {
      def decodeEntity(response: sttp.Response[String]): R[B] =
        backend.responseMonad.map(entity.decodeEntity(response))(f)
    }

  /** Successfully decodes no information from a response */
  def emptyResponse: ResponseEntity[Unit] = new ResponseEntity[Unit] {
    def decodeEntity(response: sttp.Response[String]) =
      backend.responseMonad.unit(())
  }

  /** Successfully decodes string information from a response */
  def textResponse: ResponseEntity[String] = new ResponseEntity[String] {
    def decodeEntity(response: sttp.Response[String]): R[String] =
      backend.responseMonad.unit(response.body.merge)
  }

  def stringCodecResponse[A](
      implicit codec: Codec[String, A]
  ): ResponseEntity[A] =
    sttpResponse =>
      codec.decode(sttpResponse.body.merge) match {
        case Valid(a) => backend.responseMonad.unit(a)
        case Invalid(errors) =>
          backend.responseMonad.error(new Exception(errors.mkString(". ")))
      }

  type ResponseHeaders[A] = Map[String, String] => Validated[A]

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(
          implicit tupler: Tupler[A, B]
      ): ResponseHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  implicit def responseHeadersInvariantFunctor
      : InvariantFunctor[ResponseHeaders] =
    new InvariantFunctor[ResponseHeaders] {
      def xmap[A, B](
          fa: ResponseHeaders[A],
          f: A => B,
          g: B => A
      ): ResponseHeaders[B] =
        headers => fa(headers).map(f)
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] = _ => Valid(())

  def responseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[String] =
    headers =>
      Validated.fromOption(
        headers.get(name.toLowerCase)
      )(s"Missing response header '$name'")

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] =
    headers => Valid(headers.get(name.toLowerCase))

  def response[A, B, Res](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B]
  )(
      implicit
      tupler: Tupler.Aux[A, B, Res]
  ): Response[Res] = {
    new Response[Res] {
      def decodeResponse(response: sttp.Response[String]) = {
        if (response.code == statusCode) {
          val headersMap = response.headers.iterator.map {
            case (k, v) => (k.toLowerCase, v)
          }.toMap
          headers(headersMap) match {
            case Valid(b) =>
              Some(
                mapResponseEntity(entity)(tupler(_, b)).decodeEntity(response)
              )
            case Invalid(errors) =>
              Some(
                backend.responseMonad
                  .error(new Exception(errors.mkString(". ")))
              )
          }
        } else None
      }
    }
  }

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] = {
    new Response[Either[A, B]] {
      def decodeResponse(
          response: sttp.Response[String]
      ): Option[R[Either[A, B]]] =
        responseA
          .decodeResponse(response)
          .map(backend.responseMonad.map(_)(Left(_): Either[A, B]))
          .orElse(
            responseB
              .decodeResponse(response)
              .map(backend.responseMonad.map(_)(Right(_)))
          )
    }
  }

  /**
    * A function that, given an `A`, eventually attempts to decode the `B` response.
    */
  //#endpoint-type
  type Endpoint[A, B] = A => R[B]
  //#endpoint-type

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    a => {
      val req: sttp.Request[String, Nothing] =
        request(a).response(sttp.asString)
      val result = backend.send(req)
      backend.responseMonad.flatMap(result) { sttpResponse =>
        decodeResponse(response, sttpResponse)
      }
    }

  private[client] def decodeResponse[A](
      response: Response[A],
      sttpResponse: sttp.Response[String]
  ): R[A] = {
    val maybeResponse =
      response.decodeResponse(sttpResponse)
    def maybeClientErrors =
      clientErrorsResponse
        .decodeResponse(sttpResponse)
        .map(
          backend.responseMonad.flatMap[ClientErrors, A](_)(clientErrors =>
            backend.responseMonad.error(
              new Exception(
                clientErrorsToInvalid(clientErrors).errors.mkString(". ")
              )
            )
          )
        )
    def maybeServerError =
      serverErrorResponse
        .decodeResponse(sttpResponse)
        .map(
          backend.responseMonad.flatMap[ServerError, A](_)(serverError =>
            backend.responseMonad.error(serverErrorToThrowable(serverError))
          )
        )
    maybeResponse
      .orElse(maybeClientErrors)
      .orElse(maybeServerError)
      .getOrElse(
        backend.responseMonad.error(
          new Throwable(s"Unexpected response status: ${sttpResponse.code}")
        )
      )
  }

}
