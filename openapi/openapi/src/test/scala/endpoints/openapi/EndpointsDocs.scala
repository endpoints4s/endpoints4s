package endpoints.openapi

import endpoints.algebra
import endpoints.openapi.model.{Info, OpenApi}
import io.circe.Json
import io.circe.syntax._

trait EndpointsDocs extends algebra.EndpointsDocs with Endpoints {

  //#documentation
  val api: OpenApi =
    openApi(Info(title = "API to get some resource", version = "1.0"))(
      someResource
    )
  //#documentation

  //#documentation-asjson
  val apiJson: Json = api.asJson
  //#documentation-asjson

}
