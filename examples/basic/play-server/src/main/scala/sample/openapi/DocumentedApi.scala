package sample.openapi

import endpoints.openapi.{Info, OpenApi}
import sample.algebra.Item

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with endpoints.openapi.DocumentedEndpoints
    with endpoints.openapi.DocumentedOptionalResponses
    with endpoints.openapi.DocumentedBasicAuthentication
    with endpoints.openapi.DocumentedJsonEntities {

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
