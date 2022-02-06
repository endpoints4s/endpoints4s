package cqrs.publicserver

class Router(publicServer: PublicServer, endpoints: BootstrapEndpoints) {

  val routes =
    endpoints.routes orElse publicServer.routes

}
