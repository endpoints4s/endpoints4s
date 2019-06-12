package sample

import cats.effect.IO
import endpoints.http4s.server.{BasicAuthentication, Endpoints, JsonEntitiesFromCodecs}
import org.http4s.HttpRoutes

import scala.util.Random

object Api
  extends Endpoints[IO]
    with JsonEntitiesFromCodecs
    with BasicAuthentication
    with ApiAlg {

  val router: HttpRoutes[IO] = HttpRoutes.of(
    routesFromEndpoints(
      index.implementedBy { case (name, age, _) => User(name, age) },
      maybe.implementedBy(_ =>
        if (util.Random.nextBoolean()) Some(()) else None) orElse
        action.implementedBy { _ =>
          ActionResult("Action")
        },
      actionFut.implementedByEffect { _ =>
        IO.pure(ActionResult("Action"))
      },
      auth.implementedBy { credentials =>
        println(s"Authenticated request: ${credentials.username}")
        if (Random.nextBoolean()) Some(()) else None // Randomly return a forbidden
      }
    )
  )
}
