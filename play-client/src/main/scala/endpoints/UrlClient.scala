package endpoints

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

/**
  * [[UrlAlg]] interpreter that builds URLs.
  */
trait UrlClient extends UrlAlg {

  val utf8Name = UTF_8.name()

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

  implicit lazy val stringQueryString: QueryStringValue[String] = s => URLEncoder.encode(s, utf8Name)

  implicit lazy val intQueryString: QueryStringValue[Int] = i => i.toString


  trait Segment[A] {
    def encode(a: A): String
  }

  implicit lazy val stringSegment: Segment[String] = (s: String) => URLEncoder.encode(s, utf8Name)

  implicit lazy val intSegment: Segment[Int] = (i: Int) => i.toString


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

}
