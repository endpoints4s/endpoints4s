package endpoints.algebra

import endpoints.{InvariantFunctor, Tupler}

import scala.language.{higherKinds, implicitConversions}

/**
  * Algebra interface for describing URLs made of a path and a query string.
  *
  * A path is itself made of segments chained together.
  *
  * A query string is made of named parameters.
  *
  * {{{
  *   /**
  *     * Describes an URL starting with a segment containing “articles”, followed
  *     * by another `String` segment, and a query string containing
  *     * a mandatory `Lang` parameter named “lang”, and an
  *     * optional `Int` parameter named “page”.
  *     *
  *     * Examples of matching URLs:
  *     *
  *     * - /articles/kitchen?lang=fr
  *     * - /articles/garden?lang=en&page=2
  *     */
  *   val example = path / "articles" / segment[String] /? (qs[Lang]("lang") & optQs[Int]("page"))
  * }}}
  *
  * @group algebras
  */
trait Urls {

  /** A query string carrying an `A` information */
  type QueryString[A]

  /** Provides convenient methods on [[QueryString]]. */
  implicit class QueryStringOps[A](first: QueryString[A]) {
    /**
      * Convenient method to concatenate two [[QueryString]]s.
      *
      * {{{
      *   qs[Int]("foo") & qs[String]("baz")
      * }}}
      *
      * @param second `QueryString` to concatenate with this one
      * @tparam B Information carried by the second `QueryString`
      * @return A `QueryString` that carries both `A` and `B` information
      */
    final def & [B](second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
      combineQueryStrings(first, second)
  }

  /** Concatenates two `QueryString`s */
  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out]

  /**
    * Builds a `QueryString` with one parameter.
    *
    * @param name Parameter’s name
    * @tparam A Type of the value carried by the parameter
    */
  def qs[A](name: String, docs: Documentation = None)(implicit value: QueryStringParam[A]): QueryString[A]

  /**
    * Builds a `QueryString` with one optional parameter of type `A`.
    *
    * @param name Parameter’s name
    */
  def optQs[A](name: String, docs: Documentation = None)(implicit value: QueryStringParam[A]): QueryString[Option[A]]

  /**
    * A single query string parameter carrying an `A` information.
    */
  type QueryStringParam[A]

  /** Ability to define `String` query string parameters */
  implicit def stringQueryString: QueryStringParam[String]

  /** Ability to define `Int` query string parameters */
  implicit def intQueryString: QueryStringParam[Int]

  /** Query string parameter containing a `Long` value */
  implicit def longQueryString: QueryStringParam[Long]

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

  /** Implicit conversion to get rid of intellij errors when defining paths. Effectively should not be called.*/
  implicit def dummyPathToUrl[A](p: Path[A]): Url[A] = p

  /** Convenient methods for [[Path]]s. */
  implicit class PathOps[A](first: Path[A]) {
    /** Chains this path with the `second` constant path segment */
    final def / (second: String): Path[A] = chainPaths(first, staticPathSegment(second))
    /** Chains this path with the `second` path segment */
    final def / [B](second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = chainPaths(first, second)
    /** Chains this path with the given [[QueryString]] */
    final def /? [B](qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = urlWithQueryString(first, qs)
  }

  /** Builds a static path segment */
  def  staticPathSegment(segment: String): Path[Unit]

  /** Builds a path segment carrying an `A` information */
  def segment[A](name: String = "", docs: Documentation = None)(implicit s: Segment[A]): Path[A]

  /** Chains the two paths */
  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out]

  /**
    * An empty path.
    *
    * Useful to begin a path definition:
    *
    * {{{
    *   path / "foo" / segment[Int] /? qs[String]("bar")
    * }}}
    *
    */
  val path: Path[Unit] = staticPathSegment("")

  /**
    * An URL carrying an `A` information
    */
  type Url[A]

  implicit def urlInvFunctor: InvariantFunctor[Url]


  /** Builds an URL from the given path and query string */
  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out]

}
