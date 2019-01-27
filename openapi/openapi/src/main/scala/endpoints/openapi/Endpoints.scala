package endpoints
package openapi

import endpoints.openapi.model._
import endpoints.algebra.Documentation

/**
  * Interpreter for [[algebra.Endpoints]] that produces
  * an [[OpenApi]] instance for endpoints.
  *
  * @group interpreters
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
        }).toMap
    val components = Components(
      schemas = captureSchemas(endpoints),
      securitySchemes = captureSecuritySchemes(endpoints)
    )
    OpenApi(info, items, components)
  }

  type Endpoint[A, B] = DocumentedEndpoint

  /**
    * @param path Path template (e.g. “/user/{id}”)
    * @param item Item documentation
    */
  case class DocumentedEndpoint(path: String, item: PathItem) {

    def withSecurity(securityRequirements: SecurityRequirement*): DocumentedEndpoint = {
      copy(item = PathItem(item.operations.map {
        case (verb, operation) =>
          verb -> operation.copy(security = securityRequirements.toList)
      }))
    }
  }

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
        response.map(r => r.status.toString -> Response(r.documentation, r.content)).toMap,
        tags,
        security = Nil // might be refined later by specific interpreters
      )
    val item = PathItem(Map(method -> operation))
    val path = correctPathSegments.map {
      case Left(str) => str
      case Right(param) => s"{${param.name}}"
    }.mkString("/")
    DocumentedEndpoint(path, item)
  }

  private def captureSchemas(endpoints: Iterable[DocumentedEndpoint]): Map[String, Schema] = {

    val allReferencedSchemas = for {
      documentedEndpoint <- endpoints
      operation <- documentedEndpoint.item.operations.values
      requestBodySchema = for {
        body <- operation.requestBody.toIterable
        mediaType <- body.content.values
        schema <- mediaType.schema.toIterable
      } yield schema
      responseSchemas = for {
        (_, response) <- operation.responses.toSeq
        (_, mediaType) <- response.content.toSeq
        schema <- mediaType.schema.toIterable
      } yield schema
      schema <- requestBodySchema ++ responseSchemas
      recSchema <- captureReferencedSchemasRec(schema)
    } yield recSchema

    allReferencedSchemas
      .collect { case Schema.Reference(name, Some(original), _) => name -> original }
      .toMap
  }

  private def captureReferencedSchemasRec(schema: Schema): Seq[Schema.Reference] =
    schema match {
      case Schema.Object(properties, additionalProperties, _) =>
        properties.map(_.schema).flatMap(captureReferencedSchemasRec) ++
          additionalProperties.toList.flatMap(captureReferencedSchemasRec)
      case Schema.Array(elementType, _) =>
        captureReferencedSchemasRec(elementType)
      case Schema.Enum(elementType, _, _) =>
        captureReferencedSchemasRec(elementType)
      case Schema.Primitive(_, _, _) =>
        Nil
      case Schema.OneOf(_, alternatives, _) =>
        alternatives.map(_._2).flatMap(captureReferencedSchemasRec)
      case Schema.AllOf(schemas, _) =>
        schemas.flatMap {
          case _: Schema.Reference => Nil
          case s => captureReferencedSchemasRec(s)
        }
      case referenced: Schema.Reference =>
        referenced +: referenced.original.map(captureReferencedSchemasRec).getOrElse(Nil)
    }

  private def captureSecuritySchemes(endpoints: Iterable[DocumentedEndpoint]): Map[String, SecurityScheme] = {
    endpoints
      .flatMap(_.item.operations.values)
      .flatMap(_.security)
      .map(s => s.name -> s.scheme)
      .toMap
  }
}
