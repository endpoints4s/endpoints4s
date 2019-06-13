package sample

import cats.effect.IO
import endpoints.http4s.server.JsonEntitiesFromCodec
import org.http4s.HttpRoutes

import scala.util.Random

object Api extends JsonEntitiesFromCodec[IO] with ApiAlg {

  val router: HttpRoutes[IO] = HttpRoutes.of(
    index.implementedBy { case (name, age, _) => User(name, age) } orElse
      maybe.implementedBy(_ =>
        if (util.Random.nextBoolean()) Some(()) else None) orElse
      action.implementedBy { _ =>
        ActionResult("Action")
      } orElse
      auth.implementedBy { credentials =>
        println(s"Authenticated request: ${credentials.username}")
        if (Random.nextBoolean()) Some(()) else None // Randomly return a forbidden
      }
  )
}
