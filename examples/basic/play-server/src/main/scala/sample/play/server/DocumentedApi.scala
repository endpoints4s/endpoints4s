package sample.play.server

import endpoints._

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with delegate.DocumentedEndpoints
    with delegate.DocumentedOptionalResponses
    with delegate.DocumentedBasicAuthentication {

  object delegate
    extends play.server.Endpoints
      with play.server.OptionalResponses
      with play.server.BasicAuthentication {

    val routes = routesFromEndpoints(
      item.implementedBy(id => if (id == "123abc") Some(()) else None),
      items.implementedBy(category => /* Nothing… yet! */ ()),
      admin.implementedBy(credentials => if (credentials.password == "password") Some(()) else None)
    )

  }

}
