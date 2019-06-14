package sample

import cats.implicits._
import cats.effect.{IO, Sync}
import endpoints.http4s.server.BasicAuthentication
import endpoints.http4s.server.circe.JsonEntitiesFromCodec
import org.http4s.HttpRoutes

import scala.util.Random

object Api extends JsonEntitiesFromCodec[IO] with BasicAuthentication[IO] with ApiAlg {
  lazy val F: Sync[IO] = Sync[IO]

  val router: HttpRoutes[IO] = HttpRoutes.of(
    index.implementedBy { case (name, age, _) => User(name, age) } orElse
      maybe.implementedBy(_ =>
        if (util.Random.nextBoolean()) Some(()) else None) orElse
      action.implementedBy { _ =>
        ActionResult("Action")
      }
  ) <+> HttpRoutes.of(auth.implementedBy { credentials =>
    println(s"Authenticated request: ${credentials.username}")
    if (Random.nextBoolean()) Some(()) else None // Randomly return a forbidden
  })
}
