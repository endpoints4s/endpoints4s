package endpoints.akkahttp.client

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{ HttpHeader, HttpRequest, HttpResponse, HttpEntity, Uri }
import akka.stream.Materializer
import endpoints.algebra.Documentation
import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}

import scala.concurrent.{ExecutionContext, Future}

/**
  * @group interpreters
  */
class Endpoints(val settings: EndpointsSettings)
  (implicit val EC: ExecutionContext, val M: Materializer)
  extends algebra.Endpoints
    with Urls
    with Methods
    with StatusCodes {


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
  type Response[A] = (StatusCode, scala.collection.immutable.Seq[HttpHeader]) => ResponseEntity[A]

  type ResponseEntity[A] = HttpEntity => Future[Either[Throwable, A]]

  private[client] def discardingEntity[A](result: Future[Either[Throwable, A]]): ResponseEntity[A] =
    entity => {
      entity.discardBytes() // See https://github.com/akka/akka-http/issues/1495
      result
    }

  def emptyResponse: ResponseEntity[Unit] =
    discardingEntity(Future.successful(Right(())))

  def textResponse: ResponseEntity[String] =
    entity =>
      entity.toStrict(settings.toStrictTimeout)
        .map(settings.stringContentExtractor)
        .map(Right.apply)

  def response[A](statusCode: StatusCode, responseEntity: ResponseEntity[A], docs: Documentation = None): Response[A] =
    (status, _) =>
      if (status == statusCode) responseEntity
      else discardingEntity(Future.failed(new Throwable(s"Unexpected status code: ${status.intValue()}")))

  def wheneverFound[A](inner: Response[A], notFoundDocs: Documentation): Response[Option[A]] =
    (status, headers) =>
      if (status == NotFound) discardingEntity(Future.successful(Right(None)))
      else entity => inner(status, headers)(entity).map(_.right.map(Some(_)))

  //#endpoint-type
  type Endpoint[A, B] = A => Future[B]
  //#endpoint-type

  def endpoint[A, B](request: Request[A], response: Response[B], summary: Documentation, description: Documentation, tags: List[String]): Endpoint[A, B] =
    a =>
      for {
        resp <- request(a)
        result <- response(resp.status, resp.headers)(resp.entity).flatMap(futureFromEither)
      } yield result


  private[client] def futureFromEither[A](errorOrA: Either[Throwable, A]): Future[A] =
    errorOrA match {
      case Left(error) => Future.failed(error)
      case Right(a) => Future.successful(a)
    }

}
