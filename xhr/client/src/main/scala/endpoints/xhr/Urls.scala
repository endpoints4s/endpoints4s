package endpoints.xhr

import endpoints.algebra.Documentation
import endpoints.{InvariantFunctor, Tupler, algebra}

import scala.scalajs.js

/**
  * [[algebra.Urls]] interpreter that builds URLs (in a JavaScript runtime environment).
  *
  * @group interpreters
  */
trait Urls extends algebra.Urls {

  //#segment
  /** Defines how to build a path segment from an `A` */
  trait Segment[A] {
    /** @return An URL encoded path segment (e.g. "foo%2Fbar") */
    def encode(a: A): String
  }
  //#segment

  def refineSegment[A, B](sa: Segment[A])(f: A => Option[B])(g: B => A): Segment[B] =
    (b: B) => sa.encode(g(b))

  implicit lazy val stringSegment: Segment[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  implicit lazy val intSegment: Segment[Int] =
    (i: Int) => i.toString

  implicit lazy val longSegment: Segment[Long] =
    (i: Long) => i.toString

  /**
    * Defines how to build a query string from an `A`
    */
  trait QueryString[A] {
    /** @return A query string fragment (e.g. "foo=bar&baz=a%20b") */
    def encode(a: A): String
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${first.encode(a)}&${second.encode(b)}"
    }

  def qs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[A] =
    a => s"$name=${value.encode(a)}"

  def optQs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[Option[A]] = {
    case Some(a) => qs[A](name).encode(a)
    case None => ""
  }

  /** Defines how to build a query string parameter value from an `A` */
  trait QueryStringParam[A] {
    /** @return An URL encoded query string parameter value (e.g. "a%20b") */
    def encode(a: A): String
  }

  def refineQueryStringParam[A, B](pa: QueryStringParam[A])(f: A => Option[B])(g: B => A): QueryStringParam[B] =
    (b: B) => pa.encode(g(b))

  implicit lazy val stringQueryString: QueryStringParam[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  implicit lazy val intQueryString: QueryStringParam[Int] =
    (i: Int) => i.toString

  implicit lazy val longQueryString: QueryStringParam[Long] =
    (i: Long) => i.toString

  /** Builds an URL path from an `A` */
  trait Path[A] extends Url[A]

  def staticPathSegment(segment: String) = (_: Unit) => segment

  def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] = a => s.encode(a)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    (out: tupler.Out) => {
      val (a, b) = tupler.unapply(out)
      first.encode(a) ++ "/" ++ second.encode(b)
    }

  /** Builds an URL from an `A` */
  trait Url[A] {
    def encode(a: A): String
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${path.encode(a)}?${qs.encode(b)}"
    }

  implicit val urlInvFunctor: InvariantFunctor[Url] = new InvariantFunctor[Url] {
    override def xmap[From, To](f: Url[From], map: From => To, contramap: To => From): Url[To] = new Url[To] {
      override def encode(a: To): String = f.encode(contramap(a))
    }
  }
}
