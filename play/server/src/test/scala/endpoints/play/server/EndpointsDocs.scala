package endpoints.play.server

import endpoints.algebra
import play.api.routing.Router

trait EndpointsDocs extends Endpoints with algebra.EndpointsDocs {

  //#implementation
  val routes: Router.Routes =
    routesFromEndpoints(
      someResource.implementedBy(x => s"Received $x")
    )
  //#implementation

}
