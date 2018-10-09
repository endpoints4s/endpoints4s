package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model.Schema

/**
  * Interpreter for [[algebra.Urls]]
  *
  * @group interpreters
  */
trait Urls extends algebra.Urls {

  type QueryString[A] = DocumentedQueryString

  /**
    * @param parameters List of query string parameters
    */
  case class DocumentedQueryString(parameters: List[DocumentedParameter])

  /**
    * @param name Name of the parameter
    * @param required Whether this parameter is required or not (MUST be true for path parameters)
    */
  case class DocumentedParameter(name: String, required: Boolean, description: Option[String], schema: Schema)

  def combineQueryStrings[A, B](first: QueryString[A], second: QueryString[B])(implicit tupler: Tupler[A, B]): QueryString[tupler.Out] =
    DocumentedQueryString(first.parameters ++ second.parameters)

  def qs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[A] =
    DocumentedQueryString(List(DocumentedParameter(name, required = true, docs, value)))

  def optQs[A](name: String, docs: Documentation)(implicit value: QueryStringParam[A]): QueryString[Option[A]] =
    DocumentedQueryString(List(DocumentedParameter(name, required = false, docs, value)))

  type QueryStringParam[A] = Schema

  def stringQueryString: QueryStringParam[String] = Schema.simpleString

  def intQueryString: QueryStringParam[Int] = Schema.simpleInteger

  def longQueryString: QueryStringParam[Long] = Schema.simpleInteger

  type Segment[A] = Schema

  def stringSegment: Segment[String] = Schema.simpleString

  def intSegment: Segment[Int] = Schema.simpleInteger

  def longSegment: Segment[Long] = Schema.simpleInteger

  type Path[A] = DocumentedUrl

  def staticPathSegment(segment: String): Path[Unit] = DocumentedUrl(Left(segment) :: Nil, Nil)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    DocumentedUrl(
      first.path ++ second.path,
      first.queryParameters ++ second.queryParameters // (In practice this should be empty…)
    )

  def segment[A](name: String, docs: Documentation)(implicit A: Segment[A]): Path[A] = {
    DocumentedUrl(Right(DocumentedParameter(name, required = true, docs, A)):: Nil, Nil)
  }

  type Url[A] = DocumentedUrl

  implicit lazy val urlInvFunctor: InvariantFunctor[Url] = new InvariantFunctor[Url] {
    def xmap[From, To](url: Url[From], map: From => To, contramap: To => From): Url[To] = url
  }


  /**
    * @param path List of path segments. Left is a static segment, right i path parameter
    * @param queryParameters Query string parameters
    */
  case class DocumentedUrl(path: List[Either[String, DocumentedParameter]], queryParameters: List[DocumentedParameter])

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    path.copy(queryParameters = path.queryParameters ++ qs.parameters)

}
