package sample.openapi

import endpoints.openapi.{Info, OpenApi}

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with endpoints.openapi.DocumentedEndpoints
    with endpoints.openapi.DocumentedOptionalResponses {

  /**
    * Produces an OpenAPI description of the endpoints.
    */
  val documentation: OpenApi =
    openApi(
      Info("API to get information about items", "1.0.0")
    )(
      items, item
    )

}
