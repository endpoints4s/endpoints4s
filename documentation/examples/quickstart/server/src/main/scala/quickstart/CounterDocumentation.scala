package quickstart

//#relevant-code
import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}

/** Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object CounterDocumentation
    extends CounterEndpoints
    with openapi.Endpoints
    with openapi.JsonEntitiesFromSchemas {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(currentValue, increment)

}
//#relevant-code
