package sample

import cats.effect.{IO, Sync}
import endpoints.http4s.server.BasicAuthentication
import endpoints.http4s.server.circe.JsonEntitiesFromCodec
import org.http4s.HttpRoutes

import scala.util.Random

object Api
    extends JsonEntitiesFromCodec
    with BasicAuthentication
    with ApiAlg {
  type Effect[A] = IO[A]
  lazy val Effect: Sync[IO] = Sync[IO]

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
