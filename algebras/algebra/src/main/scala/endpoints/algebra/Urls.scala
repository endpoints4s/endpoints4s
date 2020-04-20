package endpoints.algebra

import java.util.UUID

import endpoints.{
  PartialInvariantFunctor,
  PartialInvariantFunctorSyntax,
  Tupler
}

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
  *   val example = path / "articles" / segment[String]() /? (qs[Lang]("lang") & qs[Option[Int]]("page"))
  * }}}
  *
  * @group algebras
  * @groupname types Types
  * @groupdesc types Types introduced by the algebra
  * @groupprio types 1
  * @groupname operations Operations
  * @groupdesc operations Operations creating and transforming values
  * @groupprio operations 2
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
    *   - Server interpreters raise an error if they can’t parse the incoming
    *     request query string parameters as a value of type `A`. By default,
    *     they produce a Bad Request (400) response with a list of error messages
    *     in a JSON array. Refer to the documentation of your server interpreter
    *     to customize this behavior.
    *
    * @note  This type has implicit methods provided by the [[QueryStringSyntax]],
    *        [[InvariantFunctorSyntax]], and the [[PartialInvariantFunctorSyntax]] classes.
    * @group types
    */
  type QueryString[A]

  /** Provides `xmap` and `xmapPartial` operations.
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]] */
  implicit def queryStringPartialInvariantFunctor
      : PartialInvariantFunctor[QueryString]

  /** Extension methods on [[QueryString]].
    * @group operations */
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
    final def &[B](
        second: QueryString[B]
    )(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
      combineQueryStrings(first, second)
  }

  /** Concatenates two `QueryString`s */
  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(
      implicit tupler: Tupler[A, B]
  ): QueryString[tupler.Out]

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
    * @group operations
    */
  def qs[A](name: String, docs: Documentation = None)(
      implicit value: QueryStringParam[A]
  ): QueryString[A]

  /**
    * Make a query string parameter optional:
    *
    * {{{
    *   path / "articles" /? qs[Option[Int]]("page")
    * }}}
    *
    *   - Client interpreters must omit optional query string parameters that are empty.
    *   - Server interpreters must accept incoming requests whose optional query string
    *     parameters are missing, and they must report a failure for incoming requests
    *     whose optional query string parameters are present, but malformed,
    *   - Documentation interpreters should mark the parameter as optional.
    *
    * @group operations
    */
  implicit def optionalQueryStringParam[A: QueryStringParam]
      : QueryStringParam[Option[A]]

  /**
    * Support query string parameters with multiple values:
    *
    * {{{
    *   path / "articles" /? qs[List[Long]]("id")
    * }}}
    *
    *   - Server interpreters must accept incoming requests where such parameters are
    *     missing (in such a case, its value is an empty collection), and report a
    *     failure if at least one value is malformed.
    *
    * @group operations
    */
  implicit def repeatedQueryStringParam[A: QueryStringParam, CC[X] <: Iterable[
    X
  ]](implicit factory: Factory[A, CC[A]]): QueryStringParam[CC[A]]

  /**
    * A query string parameter codec for type `A`.
    *
    * The trait `Urls` provides implicit instances of type `QueryStringParam[A]`
    * for basic types (e.g., `Int`, `String`, etc.). You can create additional
    * instances by transforming or refining the existing instances with `xmap`
    * and `xmapPartial`.
    *
    * @note  This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]]
    *        and the [[InvariantFunctorSyntax]] classes.
    * @group types
    */
  type QueryStringParam[A]

  /** Provides `xmap` and `xmapPartial` operations.
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]] */
  implicit def queryStringParamPartialInvariantFunctor
      : PartialInvariantFunctor[QueryStringParam]

  /** Ability to define `String` query string parameters
    * @group operations */
  implicit def stringQueryString: QueryStringParam[String]

  /** Ability to define `UUID` query string parameters
    * @group operations */
  implicit def uuidQueryString: QueryStringParam[UUID] =
    stringQueryString.xmapWithCodec(Codec.uuidCodec)

  /** Ability to define `Int` query string parameters
    * @group operations */
  implicit def intQueryString: QueryStringParam[Int] =
    stringQueryString.xmapWithCodec(Codec.intCodec)

  /** Query string parameter containing a `Long` value
    * @group operations */
  implicit def longQueryString: QueryStringParam[Long] =
    stringQueryString.xmapWithCodec(Codec.longCodec)

  /** Query string parameter containing a `Boolean` value
    * @group operations */
  implicit def booleanQueryString: QueryStringParam[Boolean] =
    stringQueryString.xmapWithCodec(Codec.booleanCodec)

  /** Codec for query string parameters of type `Double`
    * @group operations */
  implicit def doubleQueryString: QueryStringParam[Double] =
    stringQueryString.xmapWithCodec(Codec.doubleCodec)

  /**
    * An URL path segment codec for type `A`.
    *
    * The trait `Urls` provides implicit instances of `Segment[A]` for basic types
    * (e.g., `Int`, `String`, etc.). You can create additional instances by transforming
    * or refining the existing instances with `xmap` and `xmapPartial`.
    *
    * @note  This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]]
    *        and the [[InvariantFunctorSyntax]] classes.
    * @group types
    */
  type Segment[A]

  /** Provides `xmap` and `xmapPartial` operations.
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]] */
  implicit def segmentPartialInvariantFunctor: PartialInvariantFunctor[Segment]

  /** Path segment codec for type `String`
    *
    *   - Server interpreters should return an URL-decoded string value,
    *   - Client interpreters should take an URL-decoded string value.
    * @group operations
    */
  implicit def stringSegment: Segment[String]

  /** Path segment codec for type `UUID`
    * @group operations */
  implicit def uuidSegment: Segment[UUID] =
    stringSegment.xmapWithCodec(Codec.uuidCodec)

  /** Path segment codec for type `Int`
    * @group operations */
  implicit def intSegment: Segment[Int] =
    stringSegment.xmapWithCodec(Codec.intCodec)

  /** Path segment codec for type `Long`
    * @group operations */
  implicit def longSegment: Segment[Long] =
    stringSegment.xmapWithCodec(Codec.longCodec)

  /** Path segment codec for type `Double`
    * @group operations
    */
  implicit def doubleSegment: Segment[Double] =
    stringSegment.xmapWithCodec(Codec.doubleCodec)

  /** An URL path carrying an `A` information
    *
    * Values of type `Path[A]` can be constructed by the operations [[path]],
    * [[segment]], and [[remainingSegments]].
    *
    * {{{
    *   path / "user" / segment[UUID]("id")
    * }}}
    *
    *   - Server interpreters raise an error if they can’t parse the incoming
    *     request path as a value of type `A`. By default,
    *     they produce a Bad Request (400) response with a list of error messages
    *     in a JSON array. Refer to the documentation of your server interpreter
    *     to customize this behavior.
    *
    * @note  This type has implicit methods provided by the [[PathOps]],
    *        [[InvariantFunctorSyntax]], and the [[PartialInvariantFunctorSyntax]] classes.
    * @group types */
  type Path[A] <: Url[A]

  /** Provides `xmap` and `xmapPartial` operations.
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]] */
  implicit def pathPartialInvariantFunctor: PartialInvariantFunctor[Path]

  /** Implicit conversion to get rid of intellij errors when defining paths. Effectively should not be called.
    * @see [[https://youtrack.jetbrains.com/issue/SCL-16284]] */
  private[endpoints] implicit def dummyPathToUrl[A](p: Path[A]): Url[A] = p

  /** Convenient methods for [[Path]]s.
    * @group operations */
  implicit class PathOps[A](first: Path[A]) {

    /** Chains this path with the `second` constant path segment */
    final def /(second: String): Path[A] =
      chainPaths(first, staticPathSegment(second))

    /** Chains this path with the `second` path segment */
    final def /[B](second: Path[B])(
        implicit tupler: Tupler[A, B]
    ): Path[tupler.Out] = chainPaths(first, second)

    /** Chains this path with the given [[QueryString]] */
    final def /?[B](qs: QueryString[B])(
        implicit tupler: Tupler[A, B]
    ): Url[tupler.Out] = urlWithQueryString(first, qs)
  }

  /** A path segment whose value is the given `segment`
    * @group operations */
  def staticPathSegment(segment: String): Path[Unit]

  /** A path segment carrying an `A` information
    * @group operations */
  def segment[A](name: String = "", docs: Documentation = None)(
      implicit s: Segment[A]
  ): Path[A]

  /** The remaining segments of the path. The `String` value carried by this `Path` is still URL-encoded.
    * @group operations */
  def remainingSegments(
      name: String = "",
      docs: Documentation = None
  ): Path[String] // TODO Make it impossible to chain it with another path (ie, `path / remainingSegments() / "foo"` should not compile)

  /** Chains the two paths */
  def chainPaths[A, B](first: Path[A], second: Path[B])(
      implicit tupler: Tupler[A, B]
  ): Path[tupler.Out]

  /**
    * An empty path.
    *
    * Useful to begin a path definition:
    *
    * {{{
    *   path / "foo" / segment[Int] /? qs[String]("bar")
    * }}}
    *
    * @group operations
    */
  val path: Path[Unit] = staticPathSegment("")

  /**
    * An URL carrying an `A` information
    *
    * Values of type `URL[A]` are typically constructed by first using the [[path]]
    * constructor and then chaining it with segments and query parameters.
    *
    * {{{
    *   path / "users" / segment[UUID]("id") /? qs[String]("apiKey")
    * }}}
    *
    *   - Server interpreters raise an error if they can’t parse the incoming
    *     request URL as a value of type `A`. By default,
    *     they produce a Bad Request (400) response with a list of error messages
    *     in a JSON array. Refer to the documentation of your server interpreter
    *     to customize this behavior.
    *
    * @note  This type has implicit methods provided by the [[PartialInvariantFunctorSyntax]]
    *        and [[InvariantFunctorSyntax]] classes.
    * @group types
    */
  type Url[A]

  /** Provides `xmap` and `xmapPartial` operations
    * @see [[PartialInvariantFunctorSyntax]] and [[InvariantFunctorSyntax]] */
  implicit def urlPartialInvariantFunctor: PartialInvariantFunctor[Url]

  /** Builds an URL from the given path and query string */
  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(
      implicit tupler: Tupler[A, B]
  ): Url[tupler.Out]

}
