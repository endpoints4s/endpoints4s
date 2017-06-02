package endpoints
package algebra

import endpoints.Tupler

import scala.language.higherKinds

/**
  * Algebra interface for describing URL including documentation.
  *
  * This interface is modeled after [[Urls]] but some methods
  * take additional parameters carrying the documentation part.
  */
trait DocumentedUrls {

  /**
    * An URL path segment carrying an `A` information.
    */
  type Segment[A]

  /** Ability to define `String` path segments */
  implicit def stringSegment: Segment[String]

  /** Ability to define `Int` path segments */
  implicit def intSegment: Segment[Int]

  /** Segment containing a `Long` value */
  implicit def longSegment: Segment[Long]

  /** An URL path carrying an `A` information */
  type Path[A] <: Url[A]

  /** Convenient methods for [[Path]]s. */
  implicit class PathOps[A](first: Path[A]) {
    /** Chains this path with the `second` constant path segment */
    final def / (second: String): Path[A] = chainPaths(first, staticPathSegment(second))
    /** Chains this path with the `second` path segment */
    final def / [B](second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = chainPaths(first, second)
  }

  /** Builds a static path segment */
  def staticPathSegment(segment: String): Path[Unit]

  /** Builds a path segment carrying an `A` information
    *
    * @param name Name for the segment (for documentation)
    */
  def segment[A](name: String)(implicit s: Segment[A]): Path[A]

  /** Chains the two paths */
  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out]

  /**
    * An empty path.
    *
    * Useful to begin a path definition:
    *
    * {{{
    *   path / "foo" / segment[Int]("some-value")
    * }}}
    *
    */
  val path: Path[Unit] = staticPathSegment("")

  /**
    * An URL carrying an `A` information
    */
  type Url[A]

}
