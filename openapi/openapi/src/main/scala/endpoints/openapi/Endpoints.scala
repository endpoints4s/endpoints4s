package endpoints
package openapi

import endpoints.openapi.model._

/**
  * Interpreter for [[algebra.Endpoints]] that produces an [[endpoints.openapi.model.OpenApi]] instance for endpoints,
  * and uses [[algebra.BuiltInErrors]] to model client and server errors.
  *
  * @group interpreters
  */
trait Endpoints
    extends algebra.Endpoints
    with EndpointsWithCustomErrors
    with BuiltInErrors

/**
  * Interpreter for [[algebra.Endpoints]] that produces an [[endpoints.openapi.model.OpenApi]] instance for endpoints.
  *
  * @group interpreters
  */
trait EndpointsWithCustomErrors
    extends algebra.EndpointsWithCustomErrors
    with Requests
    with Responses {

  /**
    * @return An `OpenApi` instance for the given endpoint descriptions
    * @param info      General information about the documentation to generate
    * @param endpoints The endpoints to generate the documentation for
    */
  def openApi(info: Info)(endpoints: DocumentedEndpoint*): OpenApi = {
    val items =
      endpoints
        .groupBy(_.path)
        .map {
          case (k, es) =>
            (k, es.tail.foldLeft(PathItem(es.head.item.operations)) {
              (item, e2) => PathItem(item.operations ++ e2.item.operations)
            })
        }
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

    def withSecurity(
        securityRequirements: SecurityRequirement*
    ): DocumentedEndpoint = {
      copy(item = PathItem(item.operations.map {
        case (verb, operation) =>
          verb -> operation.copy(security = securityRequirements.toList)
      }))
    }
  }

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] = {
    val method =
      request.method match {
        case Get     => "get"
        case Put     => "put"
        case Post    => "post"
        case Delete  => "delete"
        case Options => "options"
        case Patch   => "patch"
      }
    val correctPathSegments: List[Either[String, DocumentedParameter]] = {
      val prefix = "_arg"
      request.url.path
        .foldLeft((0, List[Either[String, DocumentedParameter]]())) { // (num of empty-named args, new args list)
          case ((nextEmptyArgNum, args), Right(arg)) if arg.name.isEmpty =>
            val renamed = arg.copy(name = s"$prefix$nextEmptyArgNum")
            (nextEmptyArgNum + 1, Right(renamed) :: args)
          case ((x, args), elem) => (x, elem :: args)
        }
        ._2
        .reverse
    }
    val pathParams = correctPathSegments.collect { case Right(param) => param }
    val parameters =
      pathParams.map(p =>
        Parameter(p.name, In.Path, p.required, p.description, p.schema)
      ) ++
        request.url.queryParameters.map(p =>
          Parameter(p.name, In.Query, p.required, p.description, p.schema)
        ) ++
        request.headers.value.map(h =>
          Parameter(
            h.name,
            In.Header,
            required = h.required,
            h.description,
            h.schema
          )
        )
    def responseHeaders(
        documentedHeaders: DocumentedHeaders
    ): Map[String, ResponseHeader] =
      documentedHeaders.value.map { header =>
        header.name -> ResponseHeader(
          header.required,
          header.description,
          header.schema
        )
      }.toMap
    val responses =
      (clientErrorsResponse ++ serverErrorResponse ++ response)
        .map(r =>
          r.status.toString() -> Response(
            r.documentation,
            responseHeaders(r.headers),
            r.content
          )
        )
        .toMap
    val operation =
      Operation(
        docs.summary,
        docs.description,
        parameters,
        if (request.entity.isEmpty) None
        else Some(RequestBody(request.documentation, request.entity)),
        responses,
        docs.tags,
        security = Nil, // might be refined later by specific interpreters
        docs.callbacks.map {
          case (event, callbacks) =>
            val items = callbacks.map {
              case (urlPattern, callback) =>
                val method = callback.method.toString.toLowerCase
                val requestBody =
                  RequestBody(callback.requestDocs, callback.entity.value)
                val responses =
                  callback.response.value
                    .map(r =>
                      r.status.toString() -> Response(
                        r.documentation,
                        responseHeaders(r.headers),
                        r.content
                      )
                    )
                    .toMap
                val callbackOperation =
                  Operation(
                    None,
                    None,
                    Nil,
                    Some(requestBody),
                    responses,
                    Nil,
                    Nil,
                    Map.empty,
                    deprecated = false
                  )
                (urlPattern, PathItem(Map(method -> callbackOperation)))
            }
            (event, items)
        },
        docs.deprecated
      )
    val item = PathItem(Map(method -> operation))
    val path = correctPathSegments
      .map {
        case Left(str)    => str
        case Right(param) => s"{${param.name}}"
      }
      .mkString("/")
    DocumentedEndpoint(path, item)
  }

  private def captureSchemas(
      endpoints: Iterable[DocumentedEndpoint]
  ): Map[String, Schema] = {

    val allReferencedSchemas = for {
      documentedEndpoint <- endpoints
      operations = documentedEndpoint.item.operations.values
      operation <- operations ++ operations.flatMap(
        _.callbacks.values.flatMap(_.values.flatMap(_.operations.values))
      )
      operationParametersSchemas = operation.parameters.map(_.schema)
      requestBodySchema = for {
        body <- operation.requestBody.toIterable
        mediaType <- body.content.values
        schema <- mediaType.schema.toIterable
      } yield schema
      responseSchemas = for {
        (_, response) <- operation.responses.toSeq
        (_, mediaType) <- response.content.toSeq
        schema <- mediaType.schema.toIterable ++ response.headers.values
          .map(_.schema)
      } yield schema
      schema <- requestBodySchema ++ responseSchemas ++ operationParametersSchemas
      recSchema <- captureReferencedSchemasRec(schema)
    } yield recSchema

    allReferencedSchemas.collect {
      case Schema.Reference(name, Some(original), _, _, _) => name -> original
    }.toMap
  }

  private def captureReferencedSchemasRec(
      schema: Schema
  ): Seq[Schema.Reference] =
    schema match {
      case Schema.Object(properties, additionalProperties, _, _, _) =>
        properties.map(_.schema).flatMap(captureReferencedSchemasRec) ++
          additionalProperties.toList.flatMap(captureReferencedSchemasRec)
      case Schema.Array(Left(elementType), _, _, _) =>
        captureReferencedSchemasRec(elementType)
      case Schema.Array(Right(elementTypes), _, _, _) =>
        elementTypes.flatMap(captureReferencedSchemasRec)
      case Schema.Enum(elementType, _, _, _, _) =>
        captureReferencedSchemasRec(elementType)
      case Schema.Primitive(_, _, _, _, _) =>
        Nil
      case Schema.OneOf(alternatives, _, _, _) =>
        val alternativeSchemas =
          alternatives match {
            case Schema.DiscriminatedAlternatives(_, alternatives) =>
              alternatives.map(_._2)
            case Schema.EnumeratedAlternatives(alternatives) => alternatives
          }
        alternativeSchemas.flatMap(captureReferencedSchemasRec)
      case Schema.AllOf(schemas, _, _, _) =>
        schemas.flatMap {
          case _: Schema.Reference => Nil
          case s                   => captureReferencedSchemasRec(s)
        }
      case referenced: Schema.Reference =>
        referenced +: referenced.original
          .map(captureReferencedSchemasRec)
          .getOrElse(Nil)
    }

  private def captureSecuritySchemes(
      endpoints: Iterable[DocumentedEndpoint]
  ): Map[String, SecurityScheme] = {
    endpoints
      .flatMap(_.item.operations.values)
      .flatMap(_.security)
      .map(s => s.name -> s.scheme)
      .toMap
  }
}
