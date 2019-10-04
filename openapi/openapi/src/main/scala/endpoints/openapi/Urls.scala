package endpoints
package openapi

import java.util.UUID

import endpoints.algebra.Documentation
import endpoints.openapi.model.Schema
import scala.collection.compat.Factory

import scala.language.higherKinds

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
    DocumentedQueryString(List(DocumentedParameter(name, required = value.isRequired, docs, value.schema)))

  case class DocumentedQueryStringParam(schema: Schema, isRequired: Boolean)
  type QueryStringParam[A] = DocumentedQueryStringParam

  implicit def optionalQueryStringParam[A](implicit param: QueryStringParam[A]): QueryStringParam[Option[A]] =
    param.copy(isRequired = false)

  implicit def repeatedQueryStringParam[A, CC[X] <: Iterable[X]](implicit param: QueryStringParam[A], factory: Factory[A, CC[A]]): QueryStringParam[CC[A]] =
    DocumentedQueryStringParam(Schema.Array(param.schema, description = None), isRequired = false)
  
  implicit lazy val queryStringPartialInvFunctor: PartialInvariantFunctor[QueryString] = new PartialInvariantFunctor[QueryString] {
    def xmapPartial[A, B](fa: QueryString[A], f: A => Validated[B], g: B => A): QueryString[B] = fa
  }

  implicit lazy val queryStringParamPartialInvFunctor: PartialInvariantFunctor[QueryStringParam] = new PartialInvariantFunctor[QueryStringParam] {
    def xmapPartial[A, B](fa: QueryStringParam[A], f: A => Validated[B], g: B => A): QueryStringParam[B] = fa
  }

  def stringQueryString: QueryStringParam[String] = DocumentedQueryStringParam(Schema.simpleString, isRequired = true)

  override def uuidQueryString: QueryStringParam[UUID] = DocumentedQueryStringParam(Schema.simpleUUID, isRequired = true)

  override def intQueryString: QueryStringParam[Int] = DocumentedQueryStringParam(Schema.simpleInteger, isRequired = true)

  override def longQueryString: QueryStringParam[Long] = DocumentedQueryStringParam(Schema.simpleInteger, isRequired = true)

  override def booleanQueryString: QueryStringParam[Boolean] = DocumentedQueryStringParam(Schema.simpleBoolean, isRequired = true)

  override def doubleQueryString: QueryStringParam[Double] = DocumentedQueryStringParam(Schema.simpleNumber, isRequired = true)

  type Segment[A] = Schema

  implicit lazy val segmentPartialInvFunctor: PartialInvariantFunctor[Segment] = new PartialInvariantFunctor[Segment] {
    def xmapPartial[A, B](fa: Segment[A], f: A => Validated[B], g: B => A): Segment[B] = fa
  }

  def stringSegment: Segment[String] = Schema.simpleString

  override def uuidSegment: Segment[UUID] = Schema.simpleUUID

  override def intSegment: Segment[Int] = Schema.simpleInteger

  override def longSegment: Segment[Long] = Schema.simpleInteger

  override def doubleSegment: Segment[Double] = Schema.simpleNumber

  type Path[A] = DocumentedUrl

  implicit lazy val pathPartialInvariantFunctor: PartialInvariantFunctor[Path] = new PartialInvariantFunctor[Path] {
    def xmapPartial[A, B](fa: Path[A], f: A => Validated[B], g: B => A): Path[B] = fa
  }

  def staticPathSegment(segment: String): Path[Unit] = DocumentedUrl(Left(segment) :: Nil, Nil)

  def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    DocumentedUrl(
      first.path ++ second.path,
      first.queryParameters ++ second.queryParameters // (In practice this should be empty…)
    )

  def segment[A](name: String, docs: Documentation)(implicit A: Segment[A]): Path[A] = {
    DocumentedUrl(Right(DocumentedParameter(name, required = true, docs, A)):: Nil, Nil)
  }

  def remainingSegments(name: String, docs: Documentation): Path[String] = {
    // The OpenAPI specification does not support path parameters containing slashes
    // See https://github.com/OAI/OpenAPI-Specification/issues/1459
    // Consequently, we *incorrectly* document it as a *single* string segment
    // TODO We should at least show a warning, but the error can only be detected
    // at runtime (when one uses the `remainingSegments` method *and* the openapi
    // interpreter). So, the best we could do is to log a warning.
    segment(name, docs)(stringSegment)
  }

  type Url[A] = DocumentedUrl

  implicit lazy val urlPartialInvFunctor: PartialInvariantFunctor[Url] = new PartialInvariantFunctor[Url] {
    def xmapPartial[A, B](fa: Url[A], f: A => Validated[B], g: B => A): Url[B] = fa
  }


  /**
    * @param path List of path segments. Left is a static segment, right is a path parameter
    * @param queryParameters Query string parameters
    */
  case class DocumentedUrl(path: List[Either[String, DocumentedParameter]], queryParameters: List[DocumentedParameter])

  def urlWithQueryString[A, B](path: Path[A], qs: QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] =
    path.copy(queryParameters = path.queryParameters ++ qs.parameters)

}
