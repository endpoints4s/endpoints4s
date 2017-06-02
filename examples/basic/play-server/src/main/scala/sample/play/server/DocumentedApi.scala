package sample.play.server

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with endpoints.delegate.DocumentedEndpoints {

  object delegate extends endpoints.play.server.Endpoints {

    val routes = routesFromEndpoints(
      getUser.implementedBy(id => /* Nothing… yet! */ ())
    )

  }

}
