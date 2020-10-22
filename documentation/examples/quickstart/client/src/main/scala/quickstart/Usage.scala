package quickstart

object Usage {

  //#current-value
  import scala.scalajs.js

  /** Performs an XMLHttpRequest on the `currentValue` endpoint, and then
    * deserializes the JSON response as a `Counter`.
    */
  val eventuallyCounter: js.Thenable[Counter] = CounterClient.currentValue(())
  //#current-value

  //#increment
  /** Serializes the `Increment` value into JSON and performs an XMLHttpRequest
    * on the `increment` endpoint.
    */
  val eventuallyDone: js.Thenable[Unit] = CounterClient.increment(Increment(42))
  //#increment

}
