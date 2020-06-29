package endpoints4s.play.server

import endpoints4s.algebra
import play.api.routing.Router

trait EndpointsDocs extends Endpoints with algebra.EndpointsDocs {

  //#implementation
  val routes: Router.Routes =
    routesFromEndpoints(
      someResource.implementedBy(x => s"Received $x")
    )
  //#implementation

}
