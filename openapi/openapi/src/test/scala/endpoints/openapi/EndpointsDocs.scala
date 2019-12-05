package endpoints.openapi

import endpoints.algebra.DocumentedEndpoints

//#documentation
import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}

object EndpointsDocs extends DocumentedEndpoints with openapi.Endpoints {

  val api: OpenApi =
    openApi(Info(title = "API to get some resource", version = "1.0"))(
      someDocumentedResource
    )

  //#documentation
  //#documentation-asjson
  val apiJson: String = OpenApi.stringEncoder.encode(api)
  //#documentation-asjson
  //#documentation
}
//#documentation
