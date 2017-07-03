package endpoints.play.client

import endpoints.algebra
import endpoints.algebra.{Encoder, Decoder, MuxRequest}
import endpoints.Tupler
import endpoints.play.client.Endpoints.futureFromEither
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
  * An interpreter for [[algebra.Endpoints]] that builds a client issuing requests using
  * Play’s [[WSClient]] HTTP client.
  *
  * @param host     Base of the URL of the service that implements the endpoints (e.g. "http://foo.com")
  * @param wsClient The underlying client to use
  */
class Endpoints(host: String, wsClient: WSClient)(implicit ec: ExecutionContext) extends algebra.Endpoints with Urls with Methods {

  /**
    * A function that, given an `A` and a request model, returns an updated request
    * containing additional headers
    */
  type RequestHeaders[A] = (A, WSRequest) => WSRequest

  /** Does not modify the request */
  lazy val emptyHeaders: RequestHeaders[Unit] = (_, wsRequest) => wsRequest

  /** Put both sets of headers in request */
  def joinHeaders[H1, H2](h1: RequestHeaders[H1], h2: RequestHeaders[H2])(implicit tupler: Tupler[H1, H2]): RequestHeaders[tupler.Out] = {
    (h1h2, wsRequest) => {
      val (h1p, h2p) = tupler.unapply(h1h2)
      h2(h2p, h1(h1p, wsRequest))
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

  lazy val emptyRequest: RequestEntity[Unit] = { case (_, req) => req }

  def request[A, B, C, AB](
    method: Method, url: Url[A],
    entity: RequestEntity[B], headers: RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    (abc: tuplerABC.Out) => {
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
  val emptyResponse: Response[Unit] = _ => Right(())

  /** Successfully decodes string information from a response */
  val textResponse: Response[String] = x => Right(x.body)

  //#concrete-carrier-type
  /**
    * A function that, given an `A`, eventually attempts to decode the `B` response.
    *
    * Communication failures and protocol failures are represented by a `Future.failed`.
    */
  type Endpoint[A, B] = A => Future[B]
  //#concrete-carrier-type

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] =
    a => request(a).flatMap(response andThen futureFromEither)

  class MuxEndpoint[Req <: algebra.MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ) {
    def apply(
      req: Req
    )(implicit
      encoder: Encoder[Req, Transport],
      decoder: Decoder[Transport, Resp]
    ): Future[req.Response] =
      request(encoder.encode(req)).flatMap { wsResponse =>
        futureFromEither(response(wsResponse).right.flatMap { t =>
          decoder.decode(t).asInstanceOf[Either[Throwable, req.Response]]
        })
      }
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] =
    new MuxEndpoint[Req, Resp, Transport](request, response)

}

object Endpoints {
  def futureFromEither[A](errorOrA: Either[Throwable, A]): Future[A] =
    errorOrA match {
      case Left(error) => Future.failed(error)
      case Right(a)    => Future.successful(a)
    }
}