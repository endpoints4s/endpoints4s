package endpoints4s
package openapi

import endpoints4s.algebra.Documentation
import endpoints4s.openapi.model.{MediaType, Schema}

/** Interpreter for [[algebra.Responses]]
  *
  * @group interpreters
  */
trait Responses extends algebra.Responses with StatusCodes with Headers {
  this: algebra.Errors =>

  type ResponseEntity[A] = Map[String, MediaType]

  type ResponseHeaders[A] = DocumentedHeaders

  type Response[A] = List[DocumentedResponse]

  /** @param status Response status code (e.g. OK or NotFound)
    * @param documentation Human readable documentation. Not optional because its required by openapi
    * @param headers Response headers documentation
    * @param content Map that associates each possible content-type (e.g. “text/html”) with a `MediaType` description
    */
  case class DocumentedResponse(
      status: StatusCode,
      documentation: String,
      headers: DocumentedHeaders,
      content: Map[String, MediaType]
  )

  implicit lazy val responseInvariantFunctor: InvariantFunctor[Response] =
    new InvariantFunctor[Response] {
      def xmap[A, B](fa: Response[A], f: A => B, g: B => A): Response[B] = fa
    }

  implicit lazy val responseEntityInvariantFunctor: InvariantFunctor[ResponseEntity] =
    new InvariantFunctor[ResponseEntity] {
      def xmap[A, B](
          fa: ResponseEntity[A],
          f: A => B,
          g: B => A
      ): ResponseEntity[B] = fa
    }

  def emptyResponse: ResponseEntity[Unit] = Map.empty

  def textResponse: ResponseEntity[String] =
    Map("text/plain" -> MediaType(Some(model.Schema.simpleString)))

  def response[A, B, R](
      statusCode: StatusCode,
      entity: ResponseEntity[A],
      docs: Documentation = None,
      headers: ResponseHeaders[B]
  )(implicit
      tupler: Tupler.Aux[A, B, R]
  ): Response[R] =
    DocumentedResponse(statusCode, docs.getOrElse(""), headers, entity) :: Nil

  def choiceResponse[A, B](
      responseA: Response[A],
      responseB: Response[B]
  ): Response[Either[A, B]] =
    responseA ++ responseB

  implicit def responseHeadersSemigroupal: Semigroupal[ResponseHeaders] =
    new Semigroupal[ResponseHeaders] {
      def product[A, B](fa: ResponseHeaders[A], fb: ResponseHeaders[B])(implicit
          tupler: Tupler[A, B]
      ) =
        DocumentedHeaders(fa.value ++ fb.value)
    }

  implicit def responseHeadersInvariantFunctor: InvariantFunctor[ResponseHeaders] =
    new InvariantFunctor[ResponseHeaders] {
      def xmap[A, B](
          fa: ResponseHeaders[A],
          f: A => B,
          g: B => A
      ): ResponseHeaders[B] = fa
    }

  def emptyResponseHeaders: ResponseHeaders[Unit] =
    DocumentedHeaders(Nil)

  def responseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[String] =
    DocumentedHeaders(
      List(DocumentedHeader(name, docs, required = true, Schema.simpleString))
    )

  def optResponseHeader(
      name: String,
      docs: Documentation = None
  ): ResponseHeaders[Option[String]] =
    DocumentedHeaders(
      List(DocumentedHeader(name, docs, required = false, Schema.simpleString))
    )

}
