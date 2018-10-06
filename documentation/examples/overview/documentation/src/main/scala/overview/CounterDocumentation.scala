package overview

//#relevant-code
import endpoints.openapi
import endpoints.openapi.model.{Info, OpenApi}

/**
  * Generates OpenAPI documentation for the endpoints described in the `CounterEndpoints` trait.
  */
object CounterDocumentation
  extends CounterEndpoints
    with openapi.Endpoints
    with openapi.JsonEntities {

  val api: OpenApi =
    openApi(
      Info(title = "API to manipulate a counter", version = "1.0.0")
    )(currentValue, increment)

}
//#relevant-code

object UsageExample {
  //#export
  import io.circe.syntax._
  println(CounterDocumentation.api.asJson)
  //#export
}