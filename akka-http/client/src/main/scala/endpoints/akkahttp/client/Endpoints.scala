package endpoints.akkahttp.client

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{HttpEntity, HttpHeader, HttpRequest, HttpResponse, Uri}
import akka.stream.Materializer
import endpoints.algebra.{Decoder, Documentation}
import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Akka-HTTP based interpreter that uses [[algebra.BuiltInErrors]] to model client and server errors.
  *
  * @group interpreters
  */
class Endpoints(val settings: EndpointsSettings)
  (implicit val EC: ExecutionContext, val M: Materializer)
  extends algebra.Endpoints
    with EndpointsWithCustomErrors with BuiltInErrors

/**
  * Akka-HTTP based interpreter.
  * @group interpreters
  */
trait EndpointsWithCustomErrors
  extends algebra.EndpointsWithCustomErrors
    with Urls
    with Methods
    with StatusCodes {

  def settings: EndpointsSettings
  implicit def EC: ExecutionContext
  implicit def M: Materializer

  type RequestHeaders[A] = (A, List[HttpHeader]) => List[HttpHeader]

  implicit lazy val reqHeadersInvFunctor: endpoints.InvariantFunctor[RequestHeaders] = new InvariantFunctor[RequestHeaders] {
    override def xmap[From, To](f: (From, List[HttpHeader]) => List[HttpHeader], map: From => To, contramap: To => From): (To, List[HttpHeader]) => List[HttpHeader] = {
      (to, headers) =>
        f(contramap(to), headers)
    }
  }

  implicit lazy val reqHeadersSemigroupal: Semigroupal[RequestHeaders] = new Semigroupal[RequestHeaders] {
    override def product[A, B](fa: (A, List[HttpHeader]) => List[HttpHeader], fb: (B, List[HttpHeader]) => List[HttpHeader])(implicit tupler: Tupler[A, B]): (tupler.Out, List[HttpHeader]) => List[HttpHeader] =
      (tuplerOut, headers) => {
        val (left, right) = tupler.unapply(tuplerOut)
        val leftResult = fa(left, headers)
        val rightResult = fb(right, headers)
        leftResult ++ rightResult
      }
  }

  lazy val emptyHeaders: RequestHeaders[Unit] = (_, req) => req

  case class InvalidHeaderDefinition(parsingResult: ParsingResult) extends RuntimeException

  def header(name: String, docs: Option[String]): (String, List[HttpHeader]) => List[HttpHeader] =
    (value, headers) => createHeader(name, value) :: headers

  def optHeader(name: String, docs: Option[String]): (Option[String], List[HttpHeader]) => List[HttpHeader] =
    (valueOpt, headers) => valueOpt match {
      case Some(value) => createHeader(name, value) :: headers
      case None => headers
    }

  protected def createHeader(name: String, value: String): HttpHeader =
    HttpHeader.parse(name, value) match {
      case ParsingResult.Ok(header, err) => header
      case x => throw InvalidHeaderDefinition(x)
    }


  type Request[A] = A => Future[HttpResponse]

  type RequestEntity[A] = (A, HttpRequest) => HttpRequest

  implicit lazy val reqEntityInvFunctor: endpoints.InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    override def xmap[From, To](f: (From, HttpRequest) => HttpRequest, map: From => To, contramap: To => From): (To, HttpRequest) => HttpRequest = {
      (to, req) => f(contramap(to), req)
    }
  }

  lazy val emptyRequest: RequestEntity[Unit] = (_, req) => req

  lazy val textRequest: (String, HttpRequest) => HttpRequest =
    (body, request) => request.copy(entity = HttpEntity(body))


  def request[A, B, C, AB, Out](
    method: Method, url: Url[A],
    entity: RequestEntity[B], docs: Documentation, headers: RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    (abc: Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val uri =
        if (settings.baseUri == Uri("/")) Uri(url.encode(a))
        else Uri(s"${settings.baseUri.path}${url.encode(a)}")

      val request = method(entity(b, HttpRequest(uri = uri)))
        .withHeaders(headers(c, List.empty))

      settings.requestExecutor(request)
    }

  // Defines how to decode the entity according to the status code value and response headers
  type Response[A] = (StatusCode, scala.collection.immutable.Seq[HttpHeader]) => Option[ResponseEntity[A]]

  implicit lazy val responseInvFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] =
        (status, headers) => fa(status, headers).map(mapResponseEntity(_)(f))
    }

  type ResponseEntity[A] = HttpEntity => Future[Either[Throwable, A]]

  private[client] def mapResponseEntity[A, B](entity: ResponseEntity[A])(f: A => B): ResponseEntity[B] =
    mapPartialResponseEntity(entity)(a => Right(f(a)))

  private[client] def mapPartialResponseEntity[A, B](entity: ResponseEntity[A])(f: A => Either[Throwable, B]): ResponseEntity[B] =
    httpEntity => entity(httpEntity).map(_.right.flatMap(f))

  def emptyResponse: ResponseEntity[Unit] =
    entity => {
      entity.discardBytes() // See https://github.com/akka/akka-http/issues/1495
      Future.successful(Right(()))
    }

  def textResponse: ResponseEntity[String] =
    entity =>
      entity.toStrict(settings.toStrictTimeout)
        .map(settings.stringContentExtractor)
        .map(Right.apply)

  def stringCodecResponse[A](implicit codec: Decoder[String, A]): ResponseEntity[A] = { entity =>
    for {
      strictEntity <- entity.toStrict(settings.toStrictTimeout)
    } yield {
      codec.decode(settings.stringContentExtractor(strictEntity))
        .fold(Right(_), errors => Left(new Exception(errors.mkString(". "))))
    }
  }

  def response[A](statusCode: StatusCode, responseEntity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    (status, _) =>
      if (status == statusCode) Some(responseEntity)
      else None

  def choiceResponse[A, B](responseA: Response[A], responseB: Response[B]): Response[Either[A, B]] =
    (status, headers) =>
      responseA(status, headers).map(mapResponseEntity(_)(Left(_)))
        .orElse(responseB(status, headers).map(mapResponseEntity(_)(Right(_))))

  //#endpoint-type
  type Endpoint[A, B] = A => Future[B]
  //#endpoint-type

  def endpoint[A, B](
    request: Request[A],
    response: Response[B],
    docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    a =>
      request(a).flatMap { httpResponse =>
        decodeResponse(response, httpResponse) match {
          case Some(entityB) =>
            entityB(httpResponse.entity).flatMap(futureFromEither)
          case None =>
            httpResponse.entity.discardBytes() // See https://github.com/akka/akka-http/issues/1495
            Future.failed(new Throwable(s"Unexpected response status: ${httpResponse.status.intValue()}"))
        }
      }

  // Make sure to try decoding client and error responses
  private[client] def decodeResponse[A](response: Response[A], httpResponse: HttpResponse): Option[ResponseEntity[A]] = {
    val maybeResponse = response(httpResponse.status, httpResponse.headers)
    def maybeClientErrors =
      clientErrorsResponse(httpResponse.status, httpResponse.headers)
        .map(mapPartialResponseEntity[ClientErrors, A](_)(clientErrors => Left(new Exception(clientErrorsToInvalid(clientErrors).errors.mkString(". ")))))
    def maybeServerError =
      serverErrorResponse(httpResponse.status, httpResponse.headers)
        .map(mapPartialResponseEntity[ServerError, A](_)(serverError => Left(serverErrorToThrowable(serverError))))
    maybeResponse.orElse(maybeClientErrors).orElse(maybeServerError)
  }

  private[client] def futureFromEither[A](errorOrA: Either[Throwable, A]): Future[A] =
    errorOrA match {
      case Left(error) => Future.failed(error)
      case Right(a) => Future.successful(a)
    }

}
