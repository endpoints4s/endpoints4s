package sample.openapi

import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}

object DocumentedApi
    extends sample.algebra.DocumentedApi
    with openapi.Endpoints
    with openapi.BasicAuthentication
    with openapi.JsonEntities {

  /** Produces an OpenAPI description of the endpoints.
    */
  val documentation: OpenApi =
    openApi(
      Info("API to get information about items", "1.0.0")
    )(
      items,
      item,
      admin
    )

}
