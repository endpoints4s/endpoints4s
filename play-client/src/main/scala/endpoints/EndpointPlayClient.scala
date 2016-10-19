package endpoints

import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

class EndpointPlayClient(wsClient: WSClient)(implicit ec: ExecutionContext) extends EndpointAlg with UrlClient {

  type RequestHeaders[A] = (A, WSRequest) => WSRequest

  lazy val emptyHeaders: RequestHeaders[Unit] = (_, wsRequest) => wsRequest


  type Request[A] = A => Future[WSResponse]

  type RequestEntity[A] = (A, WSRequest) => Future[WSResponse]

  def get[A, B](url: Url[A], headers: RequestHeaders[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      val wsRequest = wsClient.url(url.encode(a))
      headers(b, wsRequest).get()
    }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: RequestHeaders[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    (abc: tuplerABC.Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b) = tuplerAB.unapply(ab)
      val wsRequest = wsClient.url(url.encode(a))
      entity(b, headers(c, wsRequest))
    }


  type Response[A] = WSResponse => Either[Throwable, A]

  val emptyResponse: Response[Unit] = _ => Right(())


  type Endpoint[I, O] = I => Future[Either[Throwable, O]]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    a => request(a).map(response)

}
