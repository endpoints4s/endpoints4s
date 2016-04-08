package example

import io.circe.{Decoder, Encoder}
import julienrf.endpoints.PlayRouting
import play.api.mvc.Results

import scala.language.higherKinds

object Api extends ApiAlg with PlayRouting with Results {

  implicit def userOutput: Encoder[User] = User.enc
  implicit def actionParameterRequest: Decoder[ActionParameter] = ActionParameter.dec
  implicit def actionResultResponse: Encoder[ActionResult] = ActionResult.enc

  val routes = routesFromEndpoints(
    index.withService(name => User(name, 30)),
    action.withService(param => ActionResult(index.call("Julien").url))
  )

}
