package endpoints4s.akkahttp.server

import akka.http.scaladsl.server.Route
import endpoints4s.algebra

trait EndpointsDocs extends algebra.EndpointsDocs with Endpoints {

  //#implementation
  val route: Route =
    someResource.implementedBy(x => s"Received $x")
  //#implementation

}
