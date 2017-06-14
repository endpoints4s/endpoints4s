package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedUrls]] that ignores information
  * related to documentation and delegates to another [[algebra.Urls]]
  * interpreter.
  */
trait DocumentedUrls extends algebra.DocumentedUrls {

  val delegate: algebra.Urls

  type QueryString[A] = delegate.QueryString[A]

  /** Concatenates two `QueryString`s */
  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] = delegate.combineQueryStrings(first, second)

  /**
    * Builds a `QueryString` with one parameter.
    *
    * @param name Parameter’s name
    * @tparam A Type of the value carried by the parameter
    */
  def qs[A](name: String)(implicit value: QueryStringParam[A]): QueryString[A] = delegate.qs[A](name)

  /**
    * Builds a `QueryString` with one optional parameter of type `A`.
    *
    * @param name Parameter’s name
    */
  def optQs[A](name: String)(implicit value: QueryStringParam[A]): QueryString[Option[A]] = delegate.optQs[A](name)

  /**
    * A single query string parameter carrying an `A` information.
    */
  type QueryStringParam[A] = delegate.QueryStringParam[A]

  /** Ability to define `String` query string parameters */
  implicit def stringQueryString: QueryStringParam[String] = delegate.stringQueryString

  /** Ability to define `Int` query string parameters */
  implicit def intQueryString: QueryStringParam[Int] = delegate.intQueryString

  /** Query string parameter containing a `Long` value */
  def longQueryString: QueryStringParam[Long] = delegate.longQueryString

  type Segment[A] = delegate.Segment[A]

  implicit def stringSegment: Segment[String] = delegate.stringSegment

  implicit def intSegment: Segment[Int] = delegate.intSegment

  implicit def longSegment: Segment[Long] = delegate.longSegment

  type Path[A] = delegate.Path[A]

  def staticPathSegment(segment: String): Path[Unit] = delegate.staticPathSegment(segment)

  def segment[A](name: String)(implicit s: Segment[A]): Path[A] = delegate.segment[A]

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    delegate.chainPaths[A, B](first, second)

  type Url[A] = delegate.Url[A]

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = delegate.urlWithQueryString(path, qs)

}
