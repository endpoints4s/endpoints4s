package endpoints
package openapi

/**
  * Interpreter for [[algebra.DocumentedRequests]].
  */
trait DocumentedRequests
  extends algebra.DocumentedRequests
    with DocumentedUrls
    with Methods {

  type RequestHeaders[A] = DocumentedHeaders

  case class DocumentedHeaders(value: List[String])

  def emptyHeaders = DocumentedHeaders(Nil)

  type Request[A] = DocumentedRequest

  case class DocumentedRequest(
    method: Method,
    url: DocumentedUrl,
    headers: DocumentedHeaders,
    entity: Option[DocumentedRequestEntity]
  )

  type RequestEntity[A] = Option[DocumentedRequestEntity]

  case class DocumentedRequestEntity(description: Option[String], content: Map[String, MediaType])

  def emptyRequest = None

  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    DocumentedRequest(method, url, headers, entity)

}
