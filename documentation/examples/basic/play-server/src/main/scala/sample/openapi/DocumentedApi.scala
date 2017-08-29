package sample.openapi

import endpoints.documented.openapi
import endpoints.documented.openapi.model.{Info, OpenApi}

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with openapi.Endpoints
    with openapi.OptionalResponses
    with openapi.BasicAuthentication
    with openapi.CirceEntities {

  /**
    * Produces an OpenAPI description of the endpoints.
    */
  val documentation: OpenApi =
    openApi(
      Info("API to get information about items", "1.0.0")
    )(
      items, item, admin
    )

}
