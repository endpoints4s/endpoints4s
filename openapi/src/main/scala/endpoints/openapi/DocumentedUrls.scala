package endpoints
package openapi

/**
  * Interpreter for [[algebra.DocumentedUrls]]
  */
trait DocumentedUrls extends algebra.DocumentedUrls {

  type Url[A] = DocumentedPath
  type Path[A] = DocumentedPath
  case class DocumentedPath(path: String, parameters: List[Parameter]) // TODO Path parameters

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    DocumentedPath(first.path ++ "/" ++ second.path, first.parameters ++ second.parameters)

  def segment[A](name: String)(implicit A: Segment[A]): Path[A] =
    DocumentedPath(s"{$name}", List(Parameter(name, In.Path)))

  def staticPathSegment(segment: String): Path[Unit] = DocumentedPath(segment, Nil)

  type Segment[A] = Unit

  def stringSegment: Segment[String] = ()

  def intSegment: Segment[Int] = ()

  def longSegment: Segment[Long] = ()

}
