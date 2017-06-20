package endpoints
package openapi

import endpoints.algebra.MuxRequest

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

  type RequestHeaders[A] = List[String]

  def emptyHeaders = Nil

  type Request[A] = DocumentedRequest

  case class DocumentedRequest(
    method: Method,
    url: DocumentedUrl,
    entity: DocumentedRequestEntity
  )

  type RequestEntity[A] = DocumentedRequestEntity

  case class DocumentedRequestEntity(content: Map[String, MediaType])

  def emptyRequest = DocumentedRequestEntity(Map.empty)

  def request[A, B, C, AB](
    method: Method,
    url: Url[A],
    entity: RequestEntity[B] = emptyRequest,
    headers: RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler[AB, C]): Request[tuplerABC.Out] =
    DocumentedRequest(method, url, entity)

  type Response[A] = List[DocumentedResponse]

  case class DocumentedResponse(status: Int, description: String)

  def emptyResponse(description: String) = DocumentedResponse(200, description) :: Nil

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
    val parameters =
      request.url.pathParameters.map(p => Parameter(p.name, In.Path, p.required)) ++
      request.url.queryParameters.map(p => Parameter(p.name, In.Query, p.required))
    val operation =
      Operation(
        parameters,
        request.entity.content,
        response.map(r => r.status -> endpoints.openapi.Response(r.description)).toMap
      )
    val item = PathItem(Map(method -> operation))
    DocumentedEndpoint(request.url.path, item)
  }

  type MuxEndpoint[Req <: MuxRequest, Resp, Transport] = DocumentedEndpoint

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
    request: Request[Transport],
    response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = endpoint(request, response)

}
