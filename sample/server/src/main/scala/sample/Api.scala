package sample

import endpoints.{CirceCodecsRouting, PlayRouting}

import scala.language.higherKinds

object Api extends ApiAlg with PlayRouting with CirceCodecsRouting {

  val routes = routesFromEndpoints(
    index.implementedBy { case (name, (age, _)) => User(name, age) },
    action.implementedBy(param => ActionResult(index.call(("Julien", (30, "a&b+c"))).url))
  )

}
