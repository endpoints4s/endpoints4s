package endpoints4s.pekkohttp.server

import endpoints4s.algebra
import org.apache.pekko.http.scaladsl.server.Route

trait EndpointDefinitions extends algebra.Endpoints {
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
    endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition
}

trait EndpointsDocs extends EndpointDefinitions with Endpoints {
  //#implementation
  val route: Route =
    someResource.implementedBy(x => s"Received $x")
  //#implementation
}
