package sample.play.server

import endpoints._
import sample.algebra.Item

object DocumentedApi
  extends sample.algebra.DocumentedApi
    with delegate.DocumentedEndpoints
    with delegate.DocumentedOptionalResponses
    with delegate.DocumentedBasicAuthentication
    with delegate.DocumentedJsonEntities {

  // Note: scalac (2.11.8) crashes if I use an object definition instead of this lazy val
  lazy val delegate =
    new play.server.Endpoints
      with play.server.OptionalResponses
      with play.server.BasicAuthentication
      with play.server.circe.JsonEntities

  lazy val routes = delegate.routesFromEndpoints(
    item.implementedBy(id => if (id == "123abc") Some(Item("foo")) else None),
    items.implementedBy(category => Item("foo") :: Item("bar") :: Nil),
    admin.implementedBy(credentials => if (credentials.password == "password") Some(()) else None)
  )

  def itemDecoder = implicitly
  def listDecoder[A : JsonResponse] = implicitly

}
