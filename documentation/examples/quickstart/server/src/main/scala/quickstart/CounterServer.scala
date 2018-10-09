package quickstart

//#relevant-code
import endpoints.play.server
import endpoints.play.server.PlayComponents

import scala.concurrent.stm.Ref
import play.api.routing.Router

/**
  * Defines a Play router (and reverse router) for the endpoints described
  * in the `CounterEndpoints` trait.
  */
class CounterServer(protected val playComponents: PlayComponents)
  extends CounterEndpoints
    with server.Endpoints
    with server.circe.JsonSchemaEntities {

  /** Simple implementation of an in-memory counter */
  val counter = Ref(0)

  val routes: Router.Routes = routesFromEndpoints(

    /** Implements the `currentValue` endpoint */
    currentValue.implementedBy(_ => Counter(counter.single.get)),

    /** Implements the `increment` endpoint */
    increment.implementedBy(inc => counter.single += inc.step)

  )

}
//#relevant-code