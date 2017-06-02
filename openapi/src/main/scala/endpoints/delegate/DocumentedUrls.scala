package endpoints
package delegate

/**
  * Interpreter for [[algebra.DocumentedUrls]] that ignores information
  * related to documentation and delegates to another [[algebra.Urls]]
  * interpreter.
  */
trait DocumentedUrls extends algebra.DocumentedUrls {

  val delegate: algebra.Urls

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

}
