package example

import endpoints.{CirceCodecsRouting, PlayRouting}

import scala.language.higherKinds

object Api extends ApiAlg with PlayRouting with CirceCodecsRouting {

  val routes = routesFromEndpoints(
    index.withService(name => User(name, 30)),
    action.withService(param => ActionResult(index.call("Julien").url))
  )

}
