package quickstart

//#relevant-code
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import endpoints4s.akkahttp.server

import scala.concurrent.stm.Ref

/** Defines a Play router (and reverse router) for the endpoints described
  * in the `CounterEndpoints` trait.
  */
object CounterServer
    extends CounterEndpoints
    with server.Endpoints
    with server.JsonEntitiesFromSchemas {

  /** Simple implementation of an in-memory counter */
  val counter = Ref(0)

  // Implements the `currentValue` endpoint
  val currentValueRoute =
    currentValue.implementedBy(_ => Counter(counter.single.get))

  // Implements the `increment` endpoint
  val incrementRoute =
    //#endpoint-implementation
    increment.implementedBy(inc => counter.single += inc.step)
  //#endpoint-implementation

  val routes: Route =
    currentValueRoute ~ incrementRoute

}
//#relevant-code
