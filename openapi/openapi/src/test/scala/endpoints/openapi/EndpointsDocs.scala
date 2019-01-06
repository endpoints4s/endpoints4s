package endpoints.openapi

import endpoints.algebra

object EndpointsDocs extends algebra.EndpointsDocs with Endpoints {

  //#documentation
  import endpoints.openapi.model.{Info, OpenApi}

  val api: OpenApi =
    openApi(Info(title = "API to get some resource", version = "1.0"))(
      someDocumentedResource
    )
  //#documentation

  //#documentation-asjson
  object OpenApiEncoder
    extends endpoints.openapi.model.OpenApiSchemas
      with endpoints.circe.JsonSchemas

  import OpenApiEncoder.JsonSchema._
  import io.circe.Json
  import io.circe.syntax._

  val apiJson: Json = api.asJson
  //#documentation-asjson

}
