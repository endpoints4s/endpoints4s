package endpoints4s.openapi

import endpoints4s.algebra.DocumentedEndpoints

//#documentation
import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}

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
