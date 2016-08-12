package endpoints

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

class PlayClient(wsClient: WSClient)(implicit ec: ExecutionContext) extends EndpointsAlg {

  val utf8Name = UTF_8.name()

  trait Segment[A] {
    def encode(a: A): String
  }

  implicit lazy val stringSegment: Segment[String] =
    (s: String) => URLEncoder.encode(s, utf8Name)

  implicit lazy val intSegment: Segment[Int] =
    (i: Int) => i.toString


  trait QueryString[A] extends QueryStringOps[A] {
    def encodeQueryString(a: A): String
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${first.encodeQueryString(a)}&${second.encodeQueryString(b)}"
    }

  def qs[A](name: String)(implicit value: QueryStringValue[A]): QueryString[A] =
    a => s"$name=${value.apply(a)}"

  type QueryStringValue[A] = A => String

  implicit def stringQueryString: QueryStringValue[String] =
    s => URLEncoder.encode(s, utf8Name)

  implicit def intQueryString: QueryStringValue[Int] =
    i => i.toString


  trait Path[A] extends Url[A] with PathOps[A]

  def staticPathSegment(segment: String) = (_: Unit) => segment

  def segment[A](implicit s: Segment[A]): Path[A] = a => s.encode(a)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      first.encode(a) ++ "/" ++ second.encode(b)
    }


  trait Url[A] {
    def encode(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${path.encode(a)}?${qs.encodeQueryString(b)}"
    }

  type Headers[A] = (A, WSRequest) => WSRequest

  lazy val emptyHeaders: Headers[Unit] = (_, wsRequest) => wsRequest


  type Request[A] = A => Future[WSResponse]

  type RequestEntity[A] = (A, WSRequest) => Future[WSResponse]

  def get[A, B](url: Url[A], headers: Headers[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      val wsRequest = wsClient.url(url.encode(a))
      headers(b, wsRequest).get()
    }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: Headers[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
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
