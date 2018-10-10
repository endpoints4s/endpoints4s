package overview

//#relevant-code
import endpoints.algebra.Endpoints
import endpoints.algebra.circe.JsonEntitiesFromCodec
import io.circe.generic.JsonCodec

/**
  * Defines the HTTP endpoints description of a web service implementing a counter.
  * This web service has two endpoints: one for getting the current value of the counter,
  * and one for incrementing it.
  * It uses circe.io for JSON marshalling.
  */
trait CounterEndpoints extends Endpoints with JsonEntitiesFromCodec {

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

}

@JsonCodec
case class Counter(value: Int)

@JsonCodec
case class Increment(step: Int)
//#relevant-code