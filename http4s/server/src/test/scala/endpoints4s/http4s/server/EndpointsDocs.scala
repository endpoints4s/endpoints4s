package endpoints4s.http4s.server

import cats.effect.IO
import endpoints4s.algebra
import org.http4s.HttpRoutes

trait EndpointDefinitions extends algebra.Endpoints {
  //#endpoint-definition
  val someResource: Endpoint[Int, String] =
    endpoint(get(path / "some-resource" / segment[Int]()), ok(textResponse))
  //#endpoint-definition
}

trait EndpointsDocs extends Endpoints[IO] with EndpointDefinitions {
  //#implementation
  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      someResource.implementedBy(x => s"Received $x")
    )
  )
  //#implementation
}
