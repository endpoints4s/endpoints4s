package endpoints

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

import cats.data.Xor
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

class PlayClient(wsClient: WSClient)(implicit ec: ExecutionContext) extends EndpointsAlg {

  val utf8Name = UTF_8.name()

  type Segment[A] = A => String

  implicit def stringSegment: Segment[String] =
    (s: String) => URLEncoder.encode(s, utf8Name)

  implicit def intSegment: Segment[Int] =
    (i: Int) => i.toString


  class QueryString[A](val apply: A => String) extends QueryStringOps[A]

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit fc: FlatConcat[A, B]): QueryString[fc.Out] =
    new QueryString[fc.Out] ((ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      s"${first.apply(a)}&${second.apply(b)}"
    })

  def qs[A](name: String)(implicit value: QueryStringValue[A]): QueryString[A] =
    new QueryString(a => s"$name=${value.apply(a)}")

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

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit fc: FlatConcat[A, B]): Path[fc.Out] =
    new Path((ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      first.apply(a) ++ "/" ++ second.apply(b)
    })


  trait Url[A] {
    def encodeUrl(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit fc: FlatConcat[A, B]): Url[fc.Out] =
    (ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      s"${path.apply(a)}?${qs.apply(b)}"
    }


  type Request[A] = A => Future[WSResponse]

  type RequestEntity[A] = (A, WSRequest) => Future[WSResponse]

  def get[A](url: Url[A]) =
    a => wsClient.url(url.encodeUrl(a)).get()

  def post[A, B](url: Url[A], entity: RequestEntity[B])(implicit fc: FlatConcat[A, B]): Request[fc.Out] =
    (ab: fc.Out) => {
      val (a, b) = fc.unapply(ab)
      val wsRequest = wsClient.url(url.encodeUrl(a))
      entity(b, wsRequest)
    }


  type Response[A] = WSResponse => Xor[Throwable, A]

  val emptyResponse: Response[Unit] = _ => Xor.Right(())


  type Endpoint[I, O] = I => Future[Xor[Throwable, O]]

  def endpoint[A, B](request: Request[A], response: Response[B]) =
    a => request(a).map(response)

}
