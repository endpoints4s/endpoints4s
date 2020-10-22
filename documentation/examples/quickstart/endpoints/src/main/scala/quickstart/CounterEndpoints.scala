package quickstart

//#relevant-code
//#get-endpoint-definition
import endpoints4s.{algebra, generic}

//#get-endpoint-definition
/** Defines the HTTP endpoints description of a web service implementing a counter.
  * This web service has two endpoints: one for getting the current value of the counter,
  * and one for incrementing it.
  */
//#get-endpoint-definition
trait CounterEndpoints
    extends algebra.Endpoints
    with algebra.JsonEntitiesFromSchemas
    with generic.JsonSchemas {

//#get-endpoint-definition
  /** Get the counter current value.
    * Uses the HTTP verb “GET” and URL path “/current-value”.
    * The response entity is a JSON document representing the counter value.
    */
//#get-endpoint-definition
  val currentValue: Endpoint[Unit, Counter] =
    endpoint(get(path / "current-value"), ok(jsonResponse[Counter]))

//#get-endpoint-definition
  /** Increments the counter value.
    * Uses the HTTP verb “POST” and URL path “/increment”.
    * The request entity is a JSON document representing the increment to apply to the counter.
    * The response entity is empty.
    */
//#endpoint-definition
  val increment: Endpoint[Increment, Unit] =
    endpoint(
      post(path / "increment", jsonRequest[Increment]),
      ok(emptyResponse)
    )
//#endpoint-definition

  // Generically derive the JSON schema of our `Counter`
  // and `Increment` case classes defined thereafter
  implicit lazy val counterSchema: JsonSchema[Counter] = genericJsonSchema
  implicit lazy val incrementSchema: JsonSchema[Increment] = genericJsonSchema

//#get-endpoint-definition
}
//#get-endpoint-definition

case class Counter(value: Int)
case class Increment(step: Int)
//#relevant-code
