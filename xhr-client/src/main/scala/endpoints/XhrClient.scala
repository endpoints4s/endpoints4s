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
    new QueryString[tupler.Out]({ (ab: tupler.Out) =>
      val (a, b) = tupler.unapply(ab)
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
    new Path((out: tupler.Out) => {
      val (a, b) = tupler.unapply(out)
      first.apply(a) ++ "/" ++ second.apply(b)
    })

  trait Url[A] {
    def encodeUrl(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${path.apply(a)}?${qs.apply(b)}"
    }


  type Request[A] = js.Function1[A, (XMLHttpRequest, Option[js.Any])]

  type RequestEntity[A] = js.Function2[A, XMLHttpRequest, String /* TODO String | Blob | FormData | … */]

  def get[A](url: Url[A]) =
    a => {
      val xhr = new XMLHttpRequest
      xhr.open("GET", url.encodeUrl(a))
      (xhr, None)
    }

  def post[A, B](url: Url[A], entity: RequestEntity[B])(implicit tupler: Tupler[A, B]): Request[tupler.Out] =
    (out: tupler.Out) => {
      val (a, b) = tupler.unapply(out)
      val xhr = new XMLHttpRequest
      xhr.open("POST", url.encodeUrl(a))
      (xhr, Some(entity(b, xhr)))
    }

  type Response[A] = js.Function1[XMLHttpRequest, Either[Exception, A]]

  val emptyResponse: Response[Unit] = _ => Right(())


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
