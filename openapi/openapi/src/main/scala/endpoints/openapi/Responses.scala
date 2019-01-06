package endpoints
package openapi

import endpoints.algebra.Documentation
import endpoints.openapi.model.MediaType

/**
  * Interpreter for [[algebra.Responses]]
  *
  * @group interpreters
  */
trait Responses
  extends algebra.Responses {

  type Response[A] = List[DocumentedResponse]

  /**
    * @param status Response status code (e.g. 200)
    * @param documentation Human readable documentation. Not optional because its required by openapi
    * @param content Map that associates each possible content-type (e.g. “text/html”) with a [[MediaType]] description
    */
  case class DocumentedResponse(status: Int, documentation: String, content: Map[String, MediaType])

  def emptyResponse(docs: Documentation): Response[Unit] = DocumentedResponse(200, docs.getOrElse(""), Map.empty) :: Nil

  def textResponse(docs: Documentation): Response[String] = DocumentedResponse(200, docs.getOrElse(""), Map("text/plain" -> MediaType(None))) :: Nil

  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation): Response[Option[A]] =
    DocumentedResponse(404, notFoundDocs.getOrElse(""), content = Map.empty) :: response
}
