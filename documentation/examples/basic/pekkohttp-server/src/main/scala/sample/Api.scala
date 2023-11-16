package sample

import endpoints4s.akkahttp.server._

import scala.concurrent.Future
import scala.util.Random

object Api extends ApiAlg with Endpoints with JsonEntitiesFromCodecs with BasicAuthentication {

  import akka.http.scaladsl.server.Directives._

  val routes =
    index.implementedBy { case (name, age, _) =>
      User(name, age)
    } ~ action.implementedBy { param => ActionResult("Action") } ~ actionFut
      .implementedByAsync { param =>
        Future.successful(ActionResult("Future Action"))
      } ~
      maybe.implementedBy { _ =>
        if (util.Random.nextBoolean()) Some(()) else None
      } ~ auth.implementedBy { credentials =>
        println(s"Authenticated request: ${credentials.username}")
        if (Random.nextBoolean()) Some(())
        else None // Randomly return a forbidden
      }

}
