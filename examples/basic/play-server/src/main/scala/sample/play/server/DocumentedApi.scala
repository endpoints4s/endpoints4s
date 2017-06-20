package sample.play.server

import endpoints._

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with delegate.DocumentedEndpoints
    with delegate.DocumentedOptionalResponses {

  object delegate extends play.server.Endpoints with play.server.OptionalResponses {

    val routes = routesFromEndpoints(
      item.implementedBy(id => if (id == "123abc") Some(()) else None),
      items.implementedBy(category => /* Nothing… yet! */ ())
    )

  }

}
