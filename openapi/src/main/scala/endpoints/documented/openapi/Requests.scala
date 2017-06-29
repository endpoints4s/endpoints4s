package endpoints
package documented
package openapi

/**
  * Interpreter for [[algebra.Requests]].
  */
trait Requests
  extends algebra.Requests
    with Urls
    with Methods {

  type RequestHeaders[A] = DocumentedHeaders

  /**
    * @param value List of request header names (e.g. “Authorization”)
    */
  case class DocumentedHeaders(value: List[HeaderName])
  type HeaderName = String

  def emptyHeaders = DocumentedHeaders(Nil)

  type Request[A] = DocumentedRequest

  case class DocumentedRequest(
    method: Method,
    url: DocumentedUrl,
    headers: DocumentedHeaders,
    entity: Option[DocumentedRequestEntity]
  )

  type RequestEntity[A] = Option[DocumentedRequestEntity]

  /**
    * @param description Human readable description of the request entity
    * @param content Map that associates each possible content-type (e.g. “text/html”) with a [[MediaType]] description
    */
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
