package endpoints
package openapi

/**
  * Interpreter for [[algebra.DocumentedResponses]]
  */
trait DocumentedResponses
  extends algebra.DocumentedResponses {

  type Response[A] = List[DocumentedResponse]

  case class DocumentedResponse(status: Int, description: String, content: Map[String, MediaType])

  def emptyResponse(description: String): Response[Unit] = DocumentedResponse(200, description, Map.empty) :: Nil

  def textResponse(description: String): Response[String] = DocumentedResponse(200, description, Map("text/plain" -> MediaType(None))) :: Nil

}
