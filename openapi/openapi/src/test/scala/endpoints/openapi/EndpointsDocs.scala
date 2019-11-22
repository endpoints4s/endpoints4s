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
  import endpoints.openapi.model.OpenApi

  val apiJson: String = OpenApi.stringEncoder.encode(api)
  //#documentation-asjson

}
