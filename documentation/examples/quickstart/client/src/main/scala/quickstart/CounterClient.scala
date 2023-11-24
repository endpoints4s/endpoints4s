package quickstart

//#relevant-code
import endpoints4s.fetch
import endpoints4s.fetch.EndpointsSettings

/** Defines an HTTP client for the endpoints described in the `CounterEndpoints` trait.
  * The derived HTTP client uses XMLHttpRequest to perform requests and returns
  * results in a `js.Thenable`.
  */
object CounterClient
    extends CounterEndpoints
    with fetch.thenable.Endpoints
    with fetch.JsonEntitiesFromSchemas {
  lazy val settings: EndpointsSettings = EndpointsSettings()
}
//#relevant-code
