package endpoints4s
package openapi

import endpoints4s.algebra.Documentation
import endpoints4s.openapi.model._

/** Interpreter for [[algebra.Assets]]
  *
  * @group interpreters
  */
trait Assets extends algebra.Assets with EndpointsWithCustomErrors with StatusCodes {

  type AssetRequest = Nothing
  type AssetPath = Nothing
  type AssetResponse = Nothing

  def assetSegments(
      name: String,
      description: Option[String]
  ): Path[AssetPath] =
    DocumentedUrl(
      Right(
        DocumentedParameter(
          name,
          required = true,
          description,
          Schema.simpleString
        )
      ) :: Nil,
      Nil
    )

  def assetsEndpoint(
      url: Url[AssetPath],
      docs: Documentation,
      notFoundDocs: Documentation
  ): Endpoint[AssetRequest, AssetResponse] = {
    def response(statusCode: StatusCode) =
      DocumentedResponse(
        statusCode,
        docs.getOrElse(""),
        emptyResponseHeaders,
        Map.empty
      )
    endpoint(
      DocumentedRequest(Get, url, emptyRequestHeaders, None, emptyRequest),
      response(OK) :: response(NotModified) :: response(NotFound) :: Nil
    )
  }

  def digests: Map[String, String] = Map.empty

}
