package sample.openapi

import endpoints.documented.openapi
import endpoints.documented.openapi.{Info, OpenApi}
import sample.algebra.Item

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with openapi.Endpoints
    with openapi.OptionalResponses
    with openapi.BasicAuthentication
    with openapi.JsonEntities {

  /**
    * Produces an OpenAPI description of the endpoints.
    */
  val documentation: OpenApi =
    openApi(
      Info("API to get information about items", "1.0.0")
    )(
      items, item, admin
    )

  def itemDecoder: JsonSchema[Item] = JsonSchema.universal
  def listDecoder[A](implicit ev: JsonSchema[A]): JsonSchema[List[A]] = JsonSchema.universal
}
