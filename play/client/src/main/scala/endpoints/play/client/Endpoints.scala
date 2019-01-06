package endpoints.play.client

import endpoints.{InvariantFunctor, Semigroupal, Tupler, algebra}
import endpoints.algebra.Documentation
import endpoints.play.client.Endpoints.futureFromEither
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
  * An interpreter for [[algebra.Endpoints]] that builds a client issuing requests using
  * Play’s [[WSClient]] HTTP client.
  *
  * @param host     Base of the URL of the service that implements the endpoints (e.g. "http://foo.com")
  * @param wsClient The underlying client to use
  *
  * @group interpreters
  */
class Endpoints(host: String, wsClient: WSClient)(implicit val executionContext: ExecutionContext) extends algebra.Endpoints with Urls with Methods {

  /**
    * A function that, given an `A` and a request model, returns an updated request
    * containing additional headers
    */
  type RequestHeaders[A] = (A, WSRequest) => WSRequest

  /** Does not modify the request */
  lazy val emptyHeaders: RequestHeaders[Unit] = (_, wsRequest) => wsRequest

  def header(name: String, docs: Documentation): (String, WSRequest) => WSRequest =
    (value, req) => req.addHttpHeaders(name -> value)

  def optHeader(name: String, docs: Documentation): (Option[String], WSRequest) => WSRequest = {
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

  def textRequest(docs: Documentation): (String, WSRequest) => WSRequest =
    (body, req) => req.withBody(body)

  implicit lazy val reqEntityInvFunctor: InvariantFunctor[RequestEntity] = new InvariantFunctor[RequestEntity] {
    override def xmap[From, To](f: (From, WSRequest) => WSRequest, map: From => To, contramap: To => From): (To, WSRequest) => WSRequest =
      (to, req) => f(contramap(to), req)
  }

  def request[A, B, C, AB, Out](
    method: Method, url: Url[A],
    entity: RequestEntity[B], headers: RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    (abc: Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val wsRequest = method(entity(b, headers(c, wsClient.url(host ++ url.encode(a)))))
      wsRequest.execute()
    }

  /**
    * Attempts to decode an `A` from a `WSResponse`.
    */
  type Response[A] = WSResponse => Either[Throwable, A]

  /** Successfully decodes no information from a response */
  def emptyResponse(docs: Documentation): Response[Unit] = {
    case resp if resp.status >= 200 && resp.status < 300 => Right(())
    case resp => Left(new Throwable(s"Unexpected status code: ${resp.status}"))
  }

  /** Successfully decodes string information from a response */
  def textResponse(docs: Documentation): Response[String] = {
    case resp if resp.status >= 200 && resp.status < 300 => Right(resp.body)
    case resp => Left(new Throwable(s"Unexpected status code: ${resp.status}"))
  }

  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation): Response[Option[A]] =
    wsResponse =>
      if (wsResponse.status == 404) Right(None)
      else response(wsResponse).right.map(Some(_))

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
    summary: Documentation,
    description: Documentation,
    tags: List[String]): Endpoint[A, B] =
    a => request(a).flatMap(response andThen futureFromEither)

}

object Endpoints {
  def futureFromEither[A](errorOrA: Either[Throwable, A]): Future[A] =
    errorOrA match {
      case Left(error) => Future.failed(error)
      case Right(a) => Future.successful(a)
    }
}
