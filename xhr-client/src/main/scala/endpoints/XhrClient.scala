package endpoints

import org.scalajs.dom.XMLHttpRequest

import scala.language.higherKinds
import scala.scalajs.js

trait XhrClient extends EndpointsAlg {

  type Segment[A] = js.Function1[A, String]

  implicit def stringSegment: Segment[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  implicit def intSegment: Segment[Int] =
    (i: Int) => i.toString


  class QueryString[A](val apply: js.Function1[A, String]) extends QueryStringOps[A]

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    new QueryString[tupler.Out]({ case tupler(a, b) =>
      s"${first.apply(a)}&${second.apply(b)}"
    })

  def qs[A](name: String)(implicit value: QueryStringValue[A]): QueryString[A] =
    new QueryString(a => s"$name=${value(a)}")

  type QueryStringValue[A] = js.Function1[A, String]

  implicit def stringQueryString: QueryStringValue[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  implicit def intQueryString: QueryStringValue[Int] =
    (i: Int) => i.toString


  class Path[A](val apply: js.Function1[A, String]) extends PathOps[A] with Url[A] {
    def encodeUrl(a: A) = apply(a)
  }

  def staticPathSegment(segment: String) = new Path(_ => segment)

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
      s"${path.apply(a)}?${qs.apply(b)}"
  }


  type Headers[A] = js.Function2[A, XMLHttpRequest, Unit]

  lazy val emptyHeaders: Headers[Unit] = (_, _) => ()


  type Request[A] = js.Function1[A, (XMLHttpRequest, Option[js.Any])]

  type RequestEntity[A] = js.Function2[A, XMLHttpRequest, String /* TODO String | Blob | FormData | … */]

  def get[A, B](url: Url[A], headers: Headers[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] = {
    case tupler(a, b) =>
      val xhr = makeXhr("GET", url, a, headers, b)
      (xhr, None)
  }

  def post[A, B, C, AB](url: Url[A], entity: RequestEntity[B], headers: Headers[C])(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] = {
    case tuplerABC(tuplerAB(a, b), c) =>
      val xhr = makeXhr("POST", url, a, headers, c)
      (xhr, Some(entity(b, xhr)))
    }

  private def makeXhr[A, B](method: String, url: Url[A], a: A, headers: Headers[B], b: B): XMLHttpRequest = {
    val xhr = new XMLHttpRequest
    xhr.open(method, url.encodeUrl(a))
    headers(b, xhr)
    xhr
  }

  type Response[A] = js.Function1[XMLHttpRequest, Either[Exception, A]]

  lazy val emptyResponse: Response[Unit] = _ => Right(())


  type Task[A]

  type Endpoint[A, B] = js.Function1[A, Task[B]]

  protected final def performXhr[A, B](
    request: Request[A],
    response: Response[B],
    a: A
  )(onload: Either[Exception, B] => Unit, onerror: XMLHttpRequest => Unit): Unit = {
    val (xhr, maybeEntity) = request(a)
    xhr.onload = _ => onload(response(xhr))
    xhr.onerror = _ => onerror(xhr)
    xhr.send(maybeEntity.orNull)
  }

}
