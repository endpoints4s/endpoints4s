package endpoints

import scala.language.higherKinds

/**
  * Algebra interface for defining urls made of a path and a query string.
  *
  * A path is itself made of segments chained together.
  *
  * A query string is made of named parameters.
  */
trait UrlAlg {

  type QueryString[A] <: QueryStringOps[A]

  trait QueryStringOps[A] { first: QueryString[A] =>
    final def & [B](second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
      combineQueryStrings(first, second)
  }

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out]

  def qs[A](name: String)(implicit value: QueryStringValue[A]): QueryString[A]

  type QueryStringValue[A]

  implicit def stringQueryString: QueryStringValue[String]

  implicit def intQueryString: QueryStringValue[Int]


  type Segment[A]

  implicit def stringSegment: Segment[String]

  implicit def intSegment: Segment[Int]

  type Path[A] <: PathOps[A] with Url[A]

  trait PathOps[A] { first: Path[A] =>
    final def / (second: String): Path[A] = chainPaths(first, staticPathSegment(second))
    final def / [B](second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = chainPaths(first, second)
    final def /? [B](qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = urlWithQueryString(first, qs)
  }

  def staticPathSegment(segment: String): Path[Unit]

  def segment[A](implicit s: Segment[A]): Path[A]

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out]

  val path: Path[Unit] = staticPathSegment("")


  type Url[A]

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out]

}
