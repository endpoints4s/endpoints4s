package endpoints
package documented
package openapi

/**
  * Interpreter for [[algebra.Responses]]
  */
trait Responses
  extends algebra.Responses {

  type Response[A] = List[DocumentedResponse]

  /**
    * @param status Response status code (e.g. 200)
    * @param description Human readable description
    * @param content Map that associates each possible content-type (e.g. “text/html”) with a [[MediaType]] description
    */
  case class DocumentedResponse(status: Int, description: String, content: Map[String, MediaType])

  def emptyResponse(description: String): Response[Unit] = DocumentedResponse(200, description, Map.empty) :: Nil

  def textResponse(description: String): Response[String] = DocumentedResponse(200, description, Map("text/plain" -> MediaType(None))) :: Nil

}
