package cqrs.webclient

import endpoints4s.fetch
import endpoints4s.fetch.EndpointsSettings

object PublicEndpoints
    extends cqrs.publicserver.PublicEndpoints
    with fetch.future.Endpoints
    with fetch.JsonEntitiesFromCodecs {
  lazy val settings: EndpointsSettings = EndpointsSettings()
}
