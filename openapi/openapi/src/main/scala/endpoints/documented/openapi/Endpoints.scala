package endpoints
package documented
package openapi

import endpoints.algebra.MuxRequest
import endpoints.documented.openapi.model._

/**
  * Interpreter for [[algebra.Endpoints]] that produces
  * an [[OpenApi]] instance for endpoints.
  */
trait Endpoints
  extends algebra.Endpoints
    with Requests
    with Responses {

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

  /**
    * @param path Path template (e.g. “/user/{id}”)
    * @param item Item documentation
    */
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
      request.url.queryParameters.map(p => Parameter(p.name, In.Query, p.required)) ++
      request.headers.value.map(h => Parameter(h, In.Header, required = true))
    val operation =
      Operation(
        parameters,
        request.entity.map(r => RequestBody(r.documentation, r.content)),
        response.map(r => r.status -> Response(r.documentation, r.content)).toMap
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
