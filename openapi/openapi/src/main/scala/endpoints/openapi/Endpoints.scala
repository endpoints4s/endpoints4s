package endpoints
package openapi

import endpoints.openapi.model._
import endpoints.algebra
import endpoints.algebra.Documentation

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
    * @param info      General information about the documentation to generate
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

  def endpoint[A, B](
    request: Request[A],
    response: Response[B],
    summary: Documentation = None,
    description: Documentation = None): Endpoint[A, B] = {
    val method =
      request.method match {
        case Get => "get"
        case Put => "put"
        case Post => "post"
        case Delete => "delete"
        case Options => "options"
        case Patch => "patch"
      }
    val correctPathSegments: List[Either[String, DocumentedParameter]] = {
      val prefix = "_arg"
      request.url.path.zipWithIndex.map{
        case (Right(param), idx) if param.name.isEmpty => Right(param.copy(name = s"$prefix$idx"))
        case (x, _) => x
      }
    }
    val pathParams = correctPathSegments.collect{ case Right(param) => param}
    val parameters =
      pathParams.map(p => Parameter(p.name, In.Path, p.required, p.description, p.schema)) ++
        request.url.queryParameters.map(p => Parameter(p.name, In.Query, p.required, p.description, p.schema)) ++
        request.headers.value.map(h => Parameter(h.name, In.Header, required = h.required, h.description, h.schema))
    val operation =
      Operation(
        summary,
        description,
        parameters,
        request.entity.map(r => RequestBody(r.documentation, r.content)),
        response.map(r => r.status -> Response(r.documentation, r.content)).toMap
      )
    val item = PathItem(Map(method -> operation))
    val path = correctPathSegments.collect{
      case Left(str) => str
      case Right(param) => s"{${param.name}}"
    }.mkString("/")
    DocumentedEndpoint(path, item)
  }

}
