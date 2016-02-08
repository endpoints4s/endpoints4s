package example

import io.circe.Encoder
import julienrf.endpoints.PlayRouting
import play.api.mvc.Results

import scala.language.higherKinds

object Api extends ApiAlg with PlayRouting with Results {

  implicit def userOutput: Encoder[User] = User.enc

  val routes = routesFromEndpoints(
    index.withService(name => User(name, 30))
  )

}
