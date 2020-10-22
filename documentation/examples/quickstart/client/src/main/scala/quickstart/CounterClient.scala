package quickstart

//#relevant-code
import endpoints4s.xhr

/** Defines an HTTP client for the endpoints described in the `CounterEndpoints` trait.
  * The derived HTTP client uses XMLHttpRequest to perform requests and returns
  * results in a `js.Thenable`.
  */
object CounterClient
    extends CounterEndpoints
    with xhr.thenable.Endpoints
    with xhr.JsonEntitiesFromSchemas
//#relevant-code
