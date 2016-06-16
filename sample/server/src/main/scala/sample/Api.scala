package sample

import endpoints.{AssetsRouting, CirceCodecsRouting, PlayRouting}

import scala.concurrent.Future
import scala.language.higherKinds

object Api extends ApiAlg with PlayRouting with CirceCodecsRouting with AssetsRouting {

  val routes = routesFromEndpoints(
    index.implementedBy { case (name, (age, _)) => User(name, age) },
    action.implementedBy(param => ActionResult(index.call(("Julien", (30, "a&b+c"))).url)),
    actionFut.implementedByAsync(param => Future.successful(ActionResult(index.call(("Julien", (30, "future"))).url))),
    assets.implementedBy(assetsResources())
  )

}
