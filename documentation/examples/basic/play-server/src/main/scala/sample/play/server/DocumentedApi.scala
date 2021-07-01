package sample.play.server

import endpoints4s.play
import endpoints4s.play.server.PlayComponents
import sample.algebra.Item

class DocumentedApi(val playComponents: PlayComponents)
    extends sample.algebra.DocumentedApi
    with endpoints4s.algebra.AuthenticatedEndpointsServer
    with play.server.Endpoints
    with play.server.JsonEntitiesFromCodecs { parent =>

  lazy val routes = routesFromEndpoints(
    item.implementedBy(id => if (id == "123abc") Some(Item("foo")) else None),
    items.implementedBy(category => Item("foo") :: Item("bar") :: Nil),
    admin.implementedBy {
      case Some(credentials) if (credentials.password == "password") => Some(())
      case _                                                         => None
    }
  )

}
