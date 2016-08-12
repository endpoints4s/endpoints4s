package sample

import endpoints._

import scala.concurrent.Future

object Api extends ApiAlg with EndpointPlayRouting with CirceCodecPlayRouting with AssetPlayRouting
  with OptionalResponsePlayRouting with BasicAuthenticationPlayRouting {

  val routes = routesFromEndpoints(
    index.implementedBy { case (name, age, _) => User(name, age) },
    action.implementedBy(param => ActionResult(index.call(("Julien", 30, "a&b+c")).url)),
    actionFut.implementedByAsync(param => Future.successful(ActionResult(index.call(("Julien", 30, "future")).url))),
    assets.implementedBy(assetsResources()),
    maybe.implementedBy(_ => if (util.Random.nextBoolean()) Some(()) else None),
    auth.implementedBy { credentials => println(s"Authenticated request: ${credentials.username}"); () }
  )

}
