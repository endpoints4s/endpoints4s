package overview

//#relevant-code
import endpoints.play.server
import scala.concurrent.stm.Ref

/**
  * Defines a Play router (and reverse router) for the endpoints described in the `CounterAlg` trait.
  */
object CounterServer
  extends CounterEndpoints
    with server.Endpoints
    with server.CirceEntities {

  /** Simple implementation of an in-memory counter */
  val counter = Ref(0)

  val routes = routesFromEndpoints(

    /** Implements the `currentValue` endpoint */
    currentValue.implementedBy(_ => Counter(counter.single.get)),

    /** Implements the `increment` endpoint */
    increment.implementedBy(inc => counter.single += inc.step)

  )

}
//#relevant-code