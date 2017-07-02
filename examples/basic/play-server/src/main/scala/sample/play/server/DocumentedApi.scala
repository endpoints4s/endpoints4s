package sample.play.server

import endpoints.documented.delegate
import endpoints.play
import sample.algebra.Item

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with delegate.Endpoints
    with delegate.OptionalResponses
    with delegate.BasicAuthentication
    with delegate.CirceEntities {

  // Note: scalac (2.11.8) crashes if I use an object definition instead of this lazy val
  lazy val delegate =
    new play.server.Endpoints
      with play.server.OptionalResponses
      with play.server.BasicAuthentication
      with play.server.CirceEntities

  lazy val routes = delegate.routesFromEndpoints(
    item.implementedBy(id => if (id == "123abc") Some(Item("foo")) else None),
    items.implementedBy(category => Item("foo") :: Item("bar") :: Nil),
    admin.implementedBy(credentials => if (credentials.password == "password") Some(()) else None)
  )

}
