package cqrs.publicserver

import endpoints.play.routing.Endpoints
import play.api.routing.{Router => PlayRouter}
import play.twirl.api.Html

object BootstrapEndpoints extends Endpoints /*with Assets*/ {

  val index: Endpoint[Unit, Html] = endpoint(get(path), htmlResponse)

  val routes: PlayRouter.Routes =
    routesFromEndpoints(
      index.implementedBy(_ => Html("Hello!"))
    )

}
