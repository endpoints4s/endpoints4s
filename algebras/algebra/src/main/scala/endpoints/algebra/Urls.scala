package endpoints.algebra

import java.util.UUID

import endpoints.{Invalid, PartialInvariantFunctor, PartialInvariantFunctorSyntax, Tupler, Valid, Validated}

import scala.language.{higherKinds, implicitConversions}
import scala.collection.compat.Factory
import scala.util.{Failure, Success, Try}

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
  *   val example = path / "articles" / segment[String]() /? (qs[Lang]("lang") & qs[Option[Int]]("page"))
  * }}}
  *
  * @group algebras
  */
trait Urls extends PartialInvariantFunctorSyntax {

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

  implicit def queryStringPartialInvFunctor: PartialInvariantFunctor[QueryString]

  /** Extension methods on [[QueryString]]. */
  implicit class QueryStringSyntax[A](first: QueryString[A]) {
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

  implicit def queryStringParamPartialInvFunctor: PartialInvariantFunctor[QueryStringParam]

  def tryParseString[A](`type`: String)(parse: String => A): String => Validated[A] =
    s =>
      Try(parse(s)) match {
        case Failure(_) => Invalid(s"Invalid ${`type`} value '$s'")
        case Success(a) => Valid(a)
      }

  /** Ability to define `String` query string parameters */
  implicit def stringQueryString: QueryStringParam[String]

  /** Ability to define `UUID` query string parameters */
  implicit def uuidQueryString: QueryStringParam[UUID] =
    stringQueryString.xmapPartial(tryParseString("UUID")(UUID.fromString))(_.toString())

  /** Ability to define `Int` query string parameters */
  implicit def intQueryString: QueryStringParam[Int] =
    stringQueryString.xmapPartial(tryParseString("integer")(_.toInt))(_.toString())

  /** Query string parameter containing a `Long` value */
  implicit def longQueryString: QueryStringParam[Long] =
    stringQueryString.xmapPartial(tryParseString("integer")(_.toLong))(_.toString())

  /** Query string parameter containing a `Boolean` value */
  implicit def booleanQueryString: QueryStringParam[Boolean] =
    stringQueryString.xmapPartial[Boolean] {
      case "true"  | "1" => Valid(true)
      case "false" | "0" => Valid(false)
      case s             => Invalid(s"Invalid boolean value '$s'")
    }(_.toString())

  implicit def doubleQueryString: QueryStringParam[Double] =
    stringQueryString.xmapPartial(tryParseString("number")(_.toDouble))(_.toString())

  /**
    * An URL path segment carrying an `A` information.
    */
  type Segment[A]

  implicit def segmentPartialInvFunctor: PartialInvariantFunctor[Segment]

  /** Ability to define `String` path segments
    * Servers should return an URL-decoded string value,
    * and clients should take an URL-decoded string value.
    */
  implicit def stringSegment: Segment[String]

  /** Ability to define `UUID` path segments */
  implicit def uuidSegment: Segment[UUID] =
    stringSegment.xmapPartial(tryParseString("UUID")(UUID.fromString))(_.toString())

  /** Ability to define `Int` path segments */
  implicit def intSegment: Segment[Int] =
    stringSegment.xmapPartial(tryParseString("integer")(_.toInt))(_.toString())

  /** Segment containing a `Long` value */
  implicit def longSegment: Segment[Long] =
    stringSegment.xmapPartial(tryParseString("integer")(_.toLong))(_.toString())

  implicit def doubleSegment: Segment[Double] =
    stringSegment.xmapPartial(tryParseString("number")(_.toDouble))(_.toString())

  /** An URL path carrying an `A` information */
  type Path[A] <: Url[A]

  implicit def pathPartialInvariantFunctor: PartialInvariantFunctor[Path]

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

  /** A path segment whose value is the given `segment` */
  def staticPathSegment(segment: String): Path[Unit]

  /** A path segment carrying an `A` information */
  def segment[A](name: String = "", docs: Documentation = None)(implicit s: Segment[A]): Path[A]

  /** The remaining segments of the path. The `String` value carried by this `Path` is still URL-encoded. */
  def remainingSegments(name: String = "", docs: Documentation = None): Path[String] // TODO Make it impossible to chain it with another path (ie, `path / remainingSegments() / "foo"` should not compile)

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

  implicit def urlPartialInvFunctor: PartialInvariantFunctor[Url]

  /** Builds an URL from the given path and query string */
  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out]

}
