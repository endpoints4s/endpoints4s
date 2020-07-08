package sample

import endpoints4s.play.server._

import scala.concurrent.Future
import scala.util.Random

class Api(val playComponents: PlayComponents)
    extends ApiAlg
    with AssetsAlg
    with Endpoints
    with JsonEntitiesFromCodecs
    with Assets
    with BasicAuthentication {

  val routes = routesFromEndpoints(
    index.implementedBy { case (name, age, _) => User(name, age) },
    action.implementedBy(param => ActionResult(index.call(("Julien", 30, "a&b+c")).url)),
    actionFut.implementedByAsync(param =>
      Future.successful(ActionResult(index.call(("Julien", 30, "future")).url))
    ),
    assets.implementedBy(assetsResources()),
    maybe.implementedBy(_ => if (util.Random.nextBoolean()) Some(()) else None),
    auth.implementedBy { credentials =>
      println(s"Authenticated request: ${credentials.username}")
      if (Random.nextBoolean()) Some(())
      else None // Randomly return a forbidden
    }
  )

}
