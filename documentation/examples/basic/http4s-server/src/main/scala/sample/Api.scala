package sample

import cats.effect.IO
import endpoints4s.http4s.server.{Endpoints, JsonEntitiesFromCodecs}
import org.http4s.HttpRoutes

import scala.util.Random

object Api
    extends Endpoints[IO]
    with JsonEntitiesFromCodecs
    with endpoints4s.algebra.AuthenticatedEndpointsServer
    with ApiAlg {

  val router: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      index.implementedBy { case (name, age, _) => User(name, age) },
      maybe.implementedBy(_ => if (util.Random.nextBoolean()) Some(()) else None) orElse
        action.implementedBy { _ => ActionResult("Action") },
      actionFut.implementedByEffect { _ => IO.pure(ActionResult("Action")) },
      auth.implementedBy {
        case Some(credentials) =>
          println(s"Authenticated request: ${credentials.username}")
          if (Random.nextBoolean()) Some(())
          else None // Randomly return an unauthorized
        case None =>
          None // Missing credentials. always return an unauthorized
      }
    )
  )
}
