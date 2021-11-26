package endpoints4s.play.server

import endpoints4s.algebra
import play.api.routing.Router

trait EndpointDefinitions extends algebra.Endpoints {
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
    endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition
}

trait EndpointsDocs extends Endpoints with EndpointDefinitions {
  //#implementation
  val routes: Router.Routes =
    routesFromEndpoints(
      someResource.implementedBy(x => s"Received $x")
    )
  //#implementation
}
