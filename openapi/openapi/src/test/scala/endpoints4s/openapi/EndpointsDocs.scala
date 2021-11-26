package endpoints4s.openapi

//#documented-endpoint-definition
import endpoints4s.algebra

trait DocumentedEndpoints extends algebra.Endpoints {

  val someDocumentedResource: Endpoint[Int, String] =
    endpoint(
      get(path / "some-resource" / segment[Int]("id")),
      ok(textResponse, docs = Some("The content of the resource"))
    )

}
//#documented-endpoint-definition

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
