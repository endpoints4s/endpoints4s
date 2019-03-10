package endpoints.algebra

import java.util.UUID

import endpoints.{InvariantFunctor, Tupler}

import scala.language.{higherKinds, implicitConversions}
import scala.util.Try
import scala.collection.compat.Factory

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
  *   val example = path / "articles" / segment[String] /? (qs[Lang]("lang") & qs[Option[Int]]("page"))
  * }}}
  *
  * @group algebras
  */
trait Urls {

  /** A query string carrying an `A` information
    *
    * QueryString values can be created with the [[qs]] operation,
    * and can be combined with the `&` operation:
    *
    * {{{
    *   val queryPageAndLang: QueryString[(Int, Option[String])] =
    *     qs[Int]("page") & qs[Option[String]]("lang")
    * }}}
    *
    * */
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
    * Examples:
    *
    * {{{
    *   qs[Int]("page")            // mandatory `page` parameter
    *   qs[Option[String]]("lang") // optional `lang` parameter
    *   qs[List[Long]]("id")       // repeated `id` parameter
    * }}}
    *
    * @param name Parameter’s name
    * @tparam A Type of the value carried by the parameter
    */
  def qs[A](name: String, docs: Documentation = None)(implicit value: QueryStringParam[A]): QueryString[A]

  /**
    * Make a query string parameter optional:
    *
    * {{{
    *   path / "articles" /? qs[Option[Int]]("page")
    * }}}
    *
    * Client interpreters must omit optional query string parameters that are empty.
    * Server interpreters must accept incoming requests whose optional query string parameters are missing.
    * Server interpreters must report a failure for incoming requests whose optional query string
    * parameters are present, but malformed.
    */
  implicit def optionalQueryStringParam[A: QueryStringParam]: QueryStringParam[Option[A]]

  /**
    * Support query string parameters with multiple values:
    *
    * {{{
    *   path / "articles" /? qs[List[Long]]("id")
    * }}}
    *
    * Server interpreters must accept incoming requests where such parameters are missing (in such a
    * case, its value is an empty collection), and report a failure if at least one value is malformed.
    */
  implicit def repeatedQueryStringParam[A: QueryStringParam, CC[X] <: Iterable[X]](implicit factory: Factory[A, CC[A]]): QueryStringParam[CC[A]]

  /**
    * A single query string parameter carrying an `A` information.
    */
  type QueryStringParam[A]

  /**
    * Ability to refine a query string parameter for a type `A`
    * into a query string parameter for a type `B` given a pair
    * of decoding/encoding functions between `A` and `B`.
    *
    * @param pa A query string parameter for a type `A`
    * @param f Decoding function from `A` to `Option[B]`
    * @param g Encoding function from `B` to `A`
    * @tparam A The type of the available query string parameter.
    * @tparam B The type of the desired query string parameter.
    * @return A query string parameter for a type `B` built by refinement from `pa`.
    */
  def refineQueryStringParam[A, B](pa: QueryStringParam[A])(f: A => Option[B])(g: B => A): QueryStringParam[B]

  /** Ability to define `UUID` query string parameters */
  implicit def uuidQueryString: QueryStringParam[UUID] =
    refineQueryStringParam[String, UUID](stringQueryString)((x: String) => Try(UUID.fromString(x)).toOption)((y: UUID) => y.toString)

  /** Ability to define `String` query string parameters */
  implicit def stringQueryString: QueryStringParam[String]

  /** Ability to define `Int` query string parameters */
  implicit def intQueryString: QueryStringParam[Int] =
    refineQueryStringParam(stringQueryString)(s => Try(s.toInt).toOption)(_.toString)

  /** Query string parameter containing a `Long` value */
  implicit def longQueryString: QueryStringParam[Long] =
    refineQueryStringParam(stringQueryString)(v => Try(v.toLong).toOption)(_.toString)

  /** Query string parameter containing a `Boolean` value */
  implicit def booleanQueryString: QueryStringParam[Boolean] =
    refineQueryStringParam(stringQueryString) {
      case "true"  | "1" => Some(true)
      case "false" | "0" => Some(false)
      case _ => None
    }(_.toString)

  implicit def doubleQueryString: QueryStringParam[Double] =
    refineQueryStringParam(stringQueryString)(v => Try(v.toDouble).toOption)(_.toString)

  /**
    * An URL path segment carrying an `A` information.
    */
  type Segment[A]

  /**
    * Ability to refine a path segment for a type `A`
    * into a path segment for a type `B` given a pair
    * of decoding/encoding functions between `A` and `B`.
    *
    * @param sa A path segment for a type `A`
    * @param f Decoding function from `A` to `Option[B]`
    * @param g Encoding function from `B` to `A`
    * @tparam A The type of the available path segment.
    * @tparam B The type of the desired path segment.
    * @return A path segment for a type `B` built by refinement from `sa`.
    */
  def refineSegment[A, B](sa: Segment[A])(f: A => Option[B])(g: B => A): Segment[B]

  /** Ability to define `UUID` path segments */
  implicit def uuidSegment: Segment[UUID] =
    refineSegment[String, UUID](stringSegment)((s: String) => Try.apply(UUID.fromString(s)).toOption)(_.toString)

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
