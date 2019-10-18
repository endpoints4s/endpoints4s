package endpoints.xhr

import scala.collection.compat.Factory
import scala.language.higherKinds
import endpoints.algebra.Documentation
import endpoints.{PartialInvariantFunctor, Tupler, Validated, algebra}

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

  implicit lazy val segmentPartialInvFunctor: PartialInvariantFunctor[Segment] = new PartialInvariantFunctor[Segment] {
    def xmapPartial[A, B](fa: Segment[A], f: A => Validated[B], g: B => A): Segment[B] = (b: B) => fa.encode(g(b))
  }

  implicit lazy val stringSegment: Segment[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s)

  /**
    * Defines how to build a query string from an `A`
    */
  trait QueryString[A] {
    /** @return A query string fragment (e.g. "foo=bar&baz=a%20b") */
    def encode(a: A): Option[String]
  }

  implicit lazy val queryStringPartialInvFunctor: PartialInvariantFunctor[QueryString] = new PartialInvariantFunctor[QueryString] {
    def xmapPartial[A, B](fa: QueryString[A], f: A => Validated[B], g: B => A): QueryString[B] = (b: B) => fa.encode(g(b))
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      (first.encode(a), second.encode(b)) match {
        case (Some(left), Some(right)) => Some(s"$left&$right")
        case (Some(left), None) => Some(left)
        case (None, Some(right)) => Some(right)
        case (None, None) => None
      }
    }

  def qs[A](name: String, docs: Documentation)(implicit param: QueryStringParam[A]): QueryString[A] =
    a => {
      val params = param.encode(a)
      if (params.isEmpty) None
      else Some(params.map(v => s"$name=$v").mkString("&"))
    }

  /** Defines how to build a query string parameter value from an `A` */
  trait QueryStringParam[A] {
    /** @return An URL encoded query string parameter list of values (e.g. "a%20b") */
    def encode(a: A): List[String]
  }

  implicit lazy val queryStringParamPartialInvFunctor: PartialInvariantFunctor[QueryStringParam] = new PartialInvariantFunctor[QueryStringParam] {
    def xmapPartial[A, B](fa: QueryStringParam[A], f: A => Validated[B], g: B => A): QueryStringParam[B] =
      (b: B) => fa.encode(g(b))
  }

  implicit def optionalQueryStringParam[A](implicit param: QueryStringParam[A]): QueryStringParam[Option[A]] = {
    case Some(a) => param.encode(a)
    case None    => Nil
  }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit param: QueryStringParam[A], factory: Factory[A, CC[A]]): QueryStringParam[CC[A]] =
    as => as.iterator.flatMap(param.encode).toList

  implicit lazy val stringQueryString: QueryStringParam[String] =
    (s: String) => js.URIUtils.encodeURIComponent(s) :: Nil

  /** Builds an URL path from an `A` */
  trait Path[A] extends Url[A]

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] = new PartialInvariantFunctor[Path] {
    def xmapPartial[A, B](fa: Path[A], f: A => Validated[B], g: B => A): Path[B] = (b: B) => fa.encode(g(b))
  }

  def staticPathSegment(segment: String) = (_: Unit) => segment

  def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] = a => s.encode(a)

  def remainingSegments(name: String, docs: Documentation): Path[String] = s => s

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
      qs.encode(b) match {
        case Some(q) => s"${path.encode(a)}?$q"
        case None    => path.encode(a)
      }
    }

  implicit lazy val urlPartialInvFunctor: PartialInvariantFunctor[Url] = new PartialInvariantFunctor[Url] {
    def xmapPartial[A, B](fa: Url[A], f: A => Validated[B], g: B => A): Url[B] = (b: B) => fa.encode(g(b))
  }

}
