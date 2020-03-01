package endpoints.play.client

import endpoints.{Invalid, InvariantFunctor, PartialInvariantFunctor, Semigroupal, Tupler, Valid, Validated, algebra}
import endpoints.algebra.Documentation
import endpoints.play.client.Endpoints.futureFromEither
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
  * An interpreter for [[algebra.Endpoints]] that builds a client issuing requests using
  * Play’s `WSClient` HTTP client, and uses [[algebra.BuiltInErrors]] to model client and
  * server errors.
  *
  * @param host     Base of the URL of the service that implements the endpoints (e.g. "http://foo.com")
  * @param wsClient The underlying client to use
  *
  * @group interpreters
  */
class Endpoints(val host: String, val wsClient: WSClient)(implicit val executionContext: ExecutionContext)
  extends algebra.Endpoints with EndpointsWithCustomErrors with BuiltInErrors

/**
  * An interpreter for [[algebra.Endpoints]] that builds a client issuing requests using
  * * Play’s `WSClient` HTTP client.
  *
  * @group interpreters
  */
trait EndpointsWithCustomErrors
  extends algebra.EndpointsWithCustomErrors with Urls with Methods with StatusCodes {

  def host: String
  def wsClient: WSClient
  implicit def executionContext: ExecutionContext

  /**
    * A function that, given an `A` and a request model, returns an updated request
    * containing additional headers
    */
  type RequestHeaders[A] = (A, WSRequest) => WSRequest

  /** Does not modify the request */
  lazy val emptyRequestHeaders: RequestHeaders[Unit] = (_, wsRequest) => wsRequest

  def requestHeader(name: String, docs: Documentation): (String, WSRequest) => WSRequest =
    (value, req) => req.addHttpHeaders(name -> value)

  def optRequestHeader(name: String, docs: Documentation): (Option[String], WSRequest) => WSRequest = {
    case (Some(value), req) => req.addHttpHeaders(name -> value)
    case (None, req) => req
  }

  implicit lazy val reqHeadersInvFunctor: InvariantFunctor[RequestHeaders] = new InvariantFunctor[RequestHeaders] {
    override def xmap[From, To](f: (From, WSRequest) => WSRequest, map: From => To, contramap: To => From): (To, WSRequest) => WSRequest =
      (to, req) => f(contramap(to), req)
  }

  implicit lazy val reqHeadersSemigroupal: Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders] {
    override def product[A, B](fa: (A, WSRequest) => WSRequest, fb: (B, WSRequest) => WSRequest)(implicit tupler: Tupler[A, B]): (tupler.Out, WSRequest) => WSRequest =
      (out, req) => {
        val (a, b) = tupler.unapply(out)
        fb(b, fa(a, req))
      }
  }

  /**
    * A function that takes an `A` information and eventually returns a `WSResponse`
    */
  type Request[A] = A => Future[WSResponse]

  /**
    * A function that, given an `A` information and a `WSRequest`, eventually returns a `WSResponse`
    */
  type RequestEntity[A] = (A, WSRequest) => WSRequest

  lazy val emptyRequest: RequestEntity[Unit] = {
    case (_, req) => req
  }

  lazy val textRequest: (String, WSRequest) => WSRequest =
    (body, req) => req.withBody(body)

  implicit lazy val reqEntityInvFunctor: InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    override def xmap[From, To](f: (From, WSRequest) => WSRequest, map: From => To, contramap: To => From): (To, WSRequest) => WSRequest =
      (to, req) => f(contramap(to), req)
  }

  def request[A, B, C, AB, Out](
    method: Method, url: Url[A],
    entity: RequestEntity[B], docs: Documentation, headers: RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    (abc: Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val wsRequest = method(entity(b, headers(c, wsClient.url(s"$host${url.encode(a)}"))))
      wsRequest.execute()
    }

  /**
    * Function returning the entity decoder from the response status and headers
    */
  type Response[A] = (StatusCode, Map[String, scala.collection.Seq[String]]) => Option[ResponseEntity[A]]

  implicit lazy val responseInvFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        (status, headers) => fa(status, headers).map(mapResponseEntity(_)(f))
    }

  type ResponseEntity[A] = WSResponse => Either[Throwable, A]

  private[client] def mapResponseEntity[A, B](entity: ResponseEntity[A])(f: A => B): ResponseEntity[B] =
    mapPartialResponseEntity(entity)(a => Right(f(a)))

  private[client] def mapPartialResponseEntity[A, B](entity: ResponseEntity[A])(f: A => Either[Throwable, B]): ResponseEntity[B] =
    wsResp => entity(wsResp).flatMap(f)

  /** Discards response entity */
  def emptyResponse: ResponseEntity[Unit] =
    _ => Right(())

  /** Decodes a string entity from a response */
  def textResponse: ResponseEntity[String] =
    wsResp => Right(wsResp.body)

  type ResponseHeaders[A] = Map[String, collection.Seq[String]] => Validated[A]

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(implicit tupler: Tupler[A, B]): ResponseHeaders[tupler.Out] =
        headers => fa(headers).zip(fb(headers))
    }

  implicit def responseHeadersInvFunctor: PartialInvariantFunctor[ResponseHeaders] =
    new PartialInvariantFunctor[ResponseHeaders] {
      def xmapPartial[A, B](fa: ResponseHeaders[A], f: A => Validated[B], g: B => A): ResponseHeaders[B] =
        headers => fa(headers).flatMap(f)
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] = _ => Valid(())

  def responseHeader(name: String, docs: Documentation = None): ResponseHeaders[String] =
    headers =>
      Validated.fromOption(
        headers.get(name.toLowerCase).map(_.mkString(", "))
      )(s"Missing response header '$name'")

  def optResponseHeader(name: String, docs: Documentation = None): ResponseHeaders[Option[String]] =
    headers => Valid(headers.get(name.toLowerCase).map(_.mkString(", ")))

  def response[A, B, R](
    statusCode: StatusCode,
    entity: ResponseEntity[A],
    docs: Documentation = None,
    headers: ResponseHeaders[B] = emptyResponseHeaders
  )(implicit
    tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    (status, httpHeaders) =>
      if (status == statusCode) {
        headers(httpHeaders) match {
          case Valid(b)        => Some(mapResponseEntity(entity)(tupler(_, b)))
          case Invalid(errors) => Some(_ => Left(new Exception(errors.mkString(". "))))
        }
      } else None

  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]] =
    (status, headers) =>
      responseA(status, headers).map(mapResponseEntity(_)(Left(_)))
        .orElse(responseB(status, headers).map(mapResponseEntity(_)(Right(_))))

  /**
    * A function that, given an `A`, eventually attempts to decode the `B` response.
    *
    * Communication failures and protocol failures are represented by a `Future.failed`.
    */
  //#concrete-carrier-type
  type Endpoint[A, B] = A => Future[B]
  //#concrete-carrier-type

  def endpoint[A, B](
    request: Request[A],
    response: Response[B],
    docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    a =>
      request(a).flatMap { wsResp =>
        futureFromEither(
          decodeResponse(response, wsResp).flatMap(entity => entity(wsResp))
        )
      }

  // Make sure try decoding client error or server error responses
  private[client] def decodeResponse[A](response: Response[A], wsResponse: WSResponse): Either[Throwable, ResponseEntity[A]] = {
    val maybeResponse = response(wsResponse.status, wsResponse.headers)
    def maybeClientErrors =
      clientErrorsResponse(wsResponse.status, wsResponse.headers)
        .map(mapPartialResponseEntity[ClientErrors, A](_)(clientErrors => Left(new Exception(clientErrorsToInvalid(clientErrors).errors.mkString(". ")))))
    def maybeServerError =
      serverErrorResponse(wsResponse.status, wsResponse.headers)
        .map(mapPartialResponseEntity[ServerError, A](_)(serverError => Left(serverErrorToThrowable(serverError))))
    maybeResponse.orElse(maybeClientErrors).orElse(maybeServerError)
      .toRight(new Throwable(s"Unexpected response status: ${wsResponse.status}"))
  }
}

object Endpoints {
  def futureFromEither[A](errorOrA: Either[Throwable, A]): Future[A] =
    errorOrA match {
      case Left(error) => Future.failed(error)
      case Right(a) => Future.successful(a)
    }
}
