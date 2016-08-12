package endpoints

import scala.scalajs.js

/**
  * [[UrlAlg]] interpreter that builds URLs (in a JavaScript runtime environment).
  */
trait UrlClient extends UrlAlg {

  trait Segment[A] {
    def encode(a: A): String
  }

  implicit lazy val stringSegment: Segment[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  implicit lazy val intSegment: Segment[Int] =
    (i: Int) => i.toString


  trait QueryString[A] extends QueryStringOps[A] {
    def encode(a: A): String
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${first.encode(a)}&${second.encode(b)}"
    }

  def qs[A](name: String)(implicit value: QueryStringValue[A]): QueryString[A] =
    a => s"$name=${value.encode(a)}"

  trait QueryStringValue[A] {
    def encode(a: A): String
  }

  implicit lazy val stringQueryString: QueryStringValue[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  implicit lazy val intQueryString: QueryStringValue[Int] =
    (i: Int) => i.toString


  trait Path[A] extends Url[A] with PathOps[A]

  def staticPathSegment(segment: String) = (_: Unit) => segment

  def segment[A](implicit s: Segment[A]): Path[A] = a => s.encode(a)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    (out: tupler.Out) => {
      val (a, b) = tupler.unapply(out)
      first.encode(a) ++ "/" ++ second.encode(b)
    }

  trait Url[A] {
    def encode(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${path.encode(a)}?${qs.encode(b)}"
    }

}
