package endpoints4s.http4s.server

import cats.effect.IO
import endpoints4s.algebra
import org.http4s.HttpRoutes

trait EndpointsDocs extends Endpoints[IO] with algebra.EndpointsDocs {
  //#implementation
  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      someResource.implementedBy(x => s"Received $x")
    )
  )
  //#implementation

}
