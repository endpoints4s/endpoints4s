package endpoints.akkahttp.client

import scala.language.higherKinds
import scala.collection.compat.Factory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

import endpoints.{PartialInvariantFunctor, Tupler, Validated, algebra}
import endpoints.algebra.Documentation

/**
  * [[algebra.Urls]] interpreter that builds URLs.
  *
  * @group interpreters
  */
trait Urls extends algebra.Urls {

  val utf8Name = UTF_8.name()

  trait QueryString[A] {
    def encodeQueryString(a: A): Option[String]
  }

  implicit lazy val queryStringPartialInvFunctor: PartialInvariantFunctor[QueryString] = new PartialInvariantFunctor[QueryString] {
    def xmapPartial[A, B](fa: QueryString[A], f: A => Validated[B], g: B => A): QueryString[B] =
      (b: B) => fa.encodeQueryString(g(b))
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)

      (first.encodeQueryString(a), second.encodeQueryString(b)) match {
        case (Some(left), Some(right)) => Some(s"$left&$right")
        case (Some(left), None) => Some(left)
        case (None, Some(right)) => Some(right)
        case (None, None) => None
      }
    }

  def qs[A](name: String, docs: Documentation)(implicit param: QueryStringParam[A]): QueryString[A] =
    a => {
      val params = param(a)
      if (params.isEmpty) None
      else Some(params.map(v => s"$name=$v").mkString("&"))
    }

  /** a query string parameter can have zero or several values */
  type QueryStringParam[A] = A => List[String]

  implicit lazy val queryStringParamPartialInvFunctor: PartialInvariantFunctor[QueryStringParam] = new PartialInvariantFunctor[QueryStringParam] {
    def xmapPartial[A, B](fa: A => List[String], f: A => Validated[B], g: B => A): B => List[String] = fa compose g
  }

  implicit def optionalQueryStringParam[A](implicit param: QueryStringParam[A]): QueryStringParam[Option[A]] = {
    case Some(a) => param(a)
    case None    => Nil
  }

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit param: QueryStringParam[A], factory: Factory[A, CC[A]]): QueryStringParam[CC[A]] =
    as => as.iterator.flatMap(param).toList

  implicit lazy val stringQueryString: QueryStringParam[String] = s => URLEncoder.encode(s, utf8Name) :: Nil

  trait Segment[A] {
    def encode(a: A): String
  }

  implicit lazy val segmentPartialInvFunctor: PartialInvariantFunctor[Segment] = new PartialInvariantFunctor[Segment] {
    def xmapPartial[A, B](fa: Segment[A], f: A => Validated[B], g: B => A): Segment[B] = (b: B) => fa.encode(g(b))
  }

  implicit lazy val stringSegment: Segment[String] = (s: String) => URLEncoder.encode(s, utf8Name)

  trait Path[A] extends Url[A]

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] = new PartialInvariantFunctor[Path] {
    def xmapPartial[A, B](fa: Path[A], f: A => Validated[B], g: B => A): Path[B] = (b: B) => fa.encode(g(b))
  }

  def staticPathSegment(segment: String) = (_: Unit) => segment

  def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] = a => s.encode(a)

  def remainingSegments(name: String, docs: Documentation): Path[String] = s => s

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)
      s"${first.encode(a)}/${second.encode(b)}"
    }

  trait Url[A] {
    def encode(a: A): String
  }

  implicit lazy val urlPartialInvFunctor: PartialInvariantFunctor[Url] = new PartialInvariantFunctor[Url] {
    def xmapPartial[A, B](fa: Url[A], f: A => Validated[B], g: B => A): Url[B] = (b: B) => fa.encode(g(b))
  }

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    (ab: tupler.Out) => {
      val (a, b) = tupler.unapply(ab)

      qs.encodeQueryString(b) match {
        case Some(q) => s"${path.encode(a)}?$q"
        case None => path.encode(a)
      }
    }

}
