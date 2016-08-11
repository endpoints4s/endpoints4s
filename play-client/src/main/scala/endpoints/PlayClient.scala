package endpoints

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

class PlayClient(wsClient: WSClient)(implicit ec: ExecutionContext) extends EndpointsAlg {

  val utf8Name = UTF_8.name()

  type Segment[A] = A => String

  implicit def stringSegment: Segment[String] =
    (s: String) => URLEncoder.encode(s, utf8Name)

  implicit def intSegment: Segment[Int] =
    (i: Int) => i.toString


  trait QueryString[A] extends QueryStringOps[A] {
    def encode(a: A): String
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = {
    case tupler(a, b) =>
      s"${first.encode(a)}&${second.encode(b)}"
  }

  def qs[A](name: String)(implicit value: QueryStringValue[A]): QueryString[A] =
    a => s"$name=${value.apply(a)}"

  type QueryStringValue[A] = A => String

  implicit def stringQueryString: QueryStringValue[String] =
    s => URLEncoder.encode(s, utf8Name)

  implicit def intQueryString: QueryStringValue[Int] =
    i => i.toString


  class Path[A](val apply: A => String) extends PathOps[A] with Url[A] {
    def encodeUrl(a: A) = apply(a)
  }

  def staticPathSegment(segment: String) = new Path((_: Unit) => segment)

  def segment[A](implicit s: Segment[A]): Path[A] =
    new Path(s)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    new Path({ case tupler(a, b) =>
      first.apply(a) ++ "/" ++ second.apply(b)
    })


  trait Url[A] {
    def encodeUrl(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = {
    case tupler(a, b) =>
      s"${path.apply(a)}?${qs.encode(b)}"
  }

  type Headers[A] = (A, WSRequest) => WSRequest

  lazy val emptyHeaders: Headers[Unit] = (_, wsRequest) => wsRequest


  type Request[A] = A => Future[WSResponse]

  type RequestEntity[A] = (A, WSRequest) => Future[WSResponse]

  def get[A, B](url: Url[A], headers: Headers[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] = {
    case tupler(a, b) =>
      val wsRequest = wsClient.url(url.encodeUrl(a))
      headers(b, wsRequest).get()
  }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: Headers[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] = {
    case tuplerABC(tuplerAB(a, b), c) =>
      val wsRequest = wsClient.url(url.encodeUrl(a))
      entity(b, headers(c, wsRequest))
  }


  type Response[A] = WSResponse => Either[Throwable, A]

  val emptyResponse: Response[Unit] = _ => Right(())


  type Endpoint[I, O] = I => Future[Either[Throwable, O]]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    a => request(a).map(response)

}
