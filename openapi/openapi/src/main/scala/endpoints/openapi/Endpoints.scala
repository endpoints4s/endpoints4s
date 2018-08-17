package endpoints
package openapi

import endpoints.openapi.model._
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
    description: Documentation = None,
    tags: List[String] = Nil): Endpoint[A, B] = {
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
      request.url.path
        .foldLeft((0, List[Either[String, DocumentedParameter]]())) { // (num of empty-named args, new args list)
          case ((nextEmptyArgNum, args), Right(arg)) if arg.name.isEmpty =>
            val renamed = arg.copy(name = s"$prefix$nextEmptyArgNum")
            (nextEmptyArgNum + 1, Right(renamed) :: args)
          case ((x, args), elem) => (x, elem::args)
        }
        ._2
        .reverse
    }
    val pathParams = correctPathSegments.collect { case Right(param) => param }
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
        response.map(r => r.status -> Response(r.documentation, r.content)).toMap,
        tags
      )
    val item = PathItem(Map(method -> operation))
    val path = correctPathSegments.map {
      case Left(str) => str
      case Right(param) => s"{${param.name}}"
    }.mkString("/")
    DocumentedEndpoint(path, item)
  }

}
