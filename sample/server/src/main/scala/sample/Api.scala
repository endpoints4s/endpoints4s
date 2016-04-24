package sample

import endpoints.{CirceCodecsRouting, PlayRouting}

import scala.language.higherKinds

object Api extends ApiAlg with PlayRouting with CirceCodecsRouting {

  val routes = routesFromEndpoints(
    index.withService { case (name, (age, _)) => User(name, age) },
    action.withService(param => ActionResult(index.call(("Julien", (30, "a&b+c"))).url))
  )

}
