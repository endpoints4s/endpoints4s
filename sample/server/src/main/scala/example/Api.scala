package example

import io.circe.{Decoder, Encoder}
import julienrf.endpoints.PlayRouting
import play.api.mvc.Results

import scala.language.higherKinds

object Api extends ApiAlg with PlayRouting with Results {

  val api = new Api

  val routes = routesFromEndpoints(
    api.index.withService(name => User(name, 30)),
    api.action.withService(param => ActionResult(api.index.call("Julien").url))
  )

}
