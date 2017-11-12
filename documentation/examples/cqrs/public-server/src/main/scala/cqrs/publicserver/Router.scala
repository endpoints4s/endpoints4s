package cqrs.publicserver

import play.api.routing.{Router => PlayRouter}

class Router(publicServer: PublicServer, endpoints: BootstrapEndpoints) {

  val routes: PlayRouter.Routes =
    endpoints.routes orElse publicServer.routes

}
