package endpoints.akkahttp.client

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
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
    with Methods {


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

  def textRequest(docs: Option[String]): (String, HttpRequest) => HttpRequest =
    (body, request) => request.copy(entity = HttpEntity(body))


  def request[A, B, C, AB, Out](
    method: Method, url: Url[A],
    entity: RequestEntity[B], headers: RequestHeaders[C]
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

  type Response[A] = HttpResponse => Future[Either[Throwable, A]]

  def emptyResponse(docs: Documentation): HttpResponse => Future[Either[Throwable, Unit]] = x =>
    if (x.status == StatusCodes.OK) {
      Future.successful(Right(()))
    } else {
      Future.failed(new Throwable(s"Unexpected status code: ${x.status.intValue()}"))
    }

  def textResponse(docs: Documentation): HttpResponse => Future[Either[Throwable, String]] = x =>
    if (x.status == StatusCodes.OK) {
      x.entity.toStrict(settings.toStrictTimeout)
        .map(settings.stringContentExtractor)
        .map(Right.apply)
    } else {
      Future.failed(new Throwable(s"Unexpected status code: ${x.status.intValue()}"))
    }

  override def wheneverFound[A](inner: HttpResponse => Future[Either[Throwable, A]], notFoundDocs: Documentation): HttpResponse => Future[Either[Throwable, Option[A]]] = {
    {
      case resp if resp.status.intValue() == 404 => Future.successful(Right(None))
      case resp => inner(resp).map(_.right.map(Some(_)))
    }
  }

  //#endpoint-type
  type Endpoint[A, B] = A => Future[B]
  //#endpoint-type

  def endpoint[A, B](request: Request[A], response: Response[B], summary: Documentation, description: Documentation, tags: List[String]): Endpoint[A, B] =
    a =>
      for {
        resp <- request(a)
        result <- response(resp).flatMap(futureFromEither)
        _ = resp.discardEntityBytes() //Fix for https://github.com/akka/akka-http/issues/1495
      } yield result


  private[client] def futureFromEither[A](errorOrA: Either[Throwable, A]): Future[A] =
    errorOrA match {
      case Left(error) => Future.failed(error)
      case Right(a) => Future.successful(a)
    }

}
