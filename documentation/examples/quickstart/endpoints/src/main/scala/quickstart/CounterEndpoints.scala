package quickstart

//#relevant-code
import endpoints.{algebra, generic}

/**
  * Defines the HTTP endpoints description of a web service implementing a counter.
  * This web service has two endpoints: one for getting the current value of the counter,
  * and one for incrementing it.
  */
trait CounterEndpoints
  extends algebra.Endpoints
    with algebra.JsonSchemaEntities
    with generic.JsonSchemas {

  /**
    * Get the counter current value.
    * Uses the HTTP verb “GET” and URL path “/current-value”.
    * The response entity is a JSON document representing the counter value.
    */
  val currentValue: Endpoint[Unit, Counter] =
    endpoint(get(path / "current-value"), jsonResponse[Counter]())

  /**
    * Increments the counter value.
    * Uses the HTTP verb “POST” and URL path “/increment”.
    * The request entity is a JSON document representing the increment to apply to the counter.
    * The response entity is empty.
    */
  val increment: Endpoint[Increment, Unit] =
    endpoint(post(path / "increment", jsonRequest[Increment]()), emptyResponse())

  // Generically derive the JSON schema of our `Counter`
  // and `Increment` case classes defined thereafter
  implicit lazy val counterSchema: JsonSchema[Counter] = genericJsonSchema
  implicit lazy val incrementSchema: JsonSchema[Increment] = genericJsonSchema

}

case class Counter(value: Int)
case class Increment(step: Int)
//#relevant-code