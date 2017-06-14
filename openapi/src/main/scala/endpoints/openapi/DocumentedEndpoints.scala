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
  def openApi(info: Info)(endpoints: DocumentedEndpoint*): OpenApi = {
    val items =
      endpoints
        .groupBy(_.path)
        .mapValues(es => es.tail.foldLeft(PathItem(es.head.item.operations)) { (item, e2) =>
          PathItem(item.operations ++ e2.item.operations)
        })
    OpenApi(info, items)
  }

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
        request.url.pathParameters.map(p => Parameter(p.name, In.Path, p.required)) ++
        request.url.queryParameters.map(p => Parameter(p.name, In.Query, p.required)),
        Map(
          response.status -> endpoints.openapi.Response(response.description),
          500 -> endpoints.openapi.Response("Internal Server Error")
        )
      )
    val item = PathItem(Map(method -> operation))
    DocumentedEndpoint(request.url.path, item)
  }

  type Request[A] = DocumentedRequest
  case class DocumentedRequest(
    method: Method,
    url: DocumentedUrl
  )

  def request[A](method: Method, url: Url[A]): Request[A] =
    DocumentedRequest(method, url)

  type Response[A] = DocumentedResponse
  case class DocumentedResponse(status: Int, description: String)

  def emptyResponse(description: String) = DocumentedResponse(200, description)

}
