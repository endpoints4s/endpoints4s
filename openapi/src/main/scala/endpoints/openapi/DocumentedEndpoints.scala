package endpoints
package openapi

/**
  * Interpreter for [[algebra.DocumentedEndpoints]] that produces
  * an [[OpenApi]] instance for endpoints.
  */
trait DocumentedEndpoints
  extends algebra.DocumentedEndpoints
    with DocumentedUrls
    with Methods {

  /**
    * @return An [[OpenApi]] instance for the given endpoint descriptions
    *
    * @param info General information about the documentation to generate
    * @param endpoints The endpoints to generate the documentation for
    */
  def openApi(info: Info)(endpoints: DocumentedEndpoint*): OpenApi =
    // TODO Merge endpoints that share the same path
    OpenApi(info, endpoints.map(e => (e.path, e.item)).toMap)

  type Endpoint[A, B] = DocumentedEndpoint
  case class DocumentedEndpoint(path: String, item: PathItem)

  def endpoint[A, B](request: Request[A], response: Response[B]): Endpoint[A, B] = {
    val method =
      request.method match {
        case Get => "get"
        case Put => "put"
        case Post => "post"
        case Delete => "delete"
      }
    val operation =
      Operation(
        request.path.parameters,
        Map(
          response.status -> endpoints.openapi.Response(response.description),
          500 -> endpoints.openapi.Response("Internal Server Error")
        )
      )
    val item = PathItem(Map(method -> operation))
    DocumentedEndpoint(request.path.path, item)
  }

  type Request[A] = DocumentedRequest
  case class DocumentedRequest(method: Method, path: DocumentedPath)

  def request[A](method: Method, url: Url[A]): Request[A] = DocumentedRequest(method, url)

  type Response[A] = DocumentedResponse
  case class DocumentedResponse(status: Int, description: String)

  def emptyResponse(description: String) = DocumentedResponse(200, description)

}
