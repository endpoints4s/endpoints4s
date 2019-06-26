package endpoints.http4s.server

import cats.effect.{IO, Sync}
import endpoints.algebra
import org.http4s.HttpRoutes

trait EndpointsDocs extends Endpoints with algebra.EndpointsDocs {
  //#implementation
  type Effect[A] = IO[A]
  lazy val Effect: Sync[IO] = Sync[IO]

  val routes: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      someResource.implementedBy(x => s"Received $x")
    ))
  //#implementation

}
