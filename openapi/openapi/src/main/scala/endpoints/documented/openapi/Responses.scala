package endpoints
package documented
package openapi

import endpoints.documented.openapi.model.MediaType

/**
  * Interpreter for [[algebra.Responses]]
  */
trait Responses
  extends algebra.Responses {

  type Response[A] = List[DocumentedResponse]

  /**
    * @param status Response status code (e.g. 200)
    * @param documentation Human readable documentation
    * @param content Map that associates each possible content-type (e.g. “text/html”) with a [[MediaType]] description
    */
  case class DocumentedResponse(status: Int, documentation: String, content: Map[String, MediaType])

  def emptyResponse(documentation: String): Response[Unit] = DocumentedResponse(200, documentation, Map.empty) :: Nil

  def textResponse(documentation: String): Response[String] = DocumentedResponse(200, documentation, Map("text/plain" -> MediaType(None))) :: Nil

}
