package cqrs.webclient

import endpoints4s.xhr
import endpoints4s.xhr.EndpointsSettings

object PublicEndpoints
    extends cqrs.publicserver.PublicEndpoints
    with xhr.faithful.Endpoints
    with xhr.JsonEntitiesFromCodecs {
  val settings: EndpointsSettings = EndpointsSettings()
}
