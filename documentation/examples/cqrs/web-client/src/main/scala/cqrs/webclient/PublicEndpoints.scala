package cqrs.webclient

import endpoints.xhr

object PublicEndpoints
  extends cqrs.publicserver.PublicEndpoints
    with xhr.faithful.Endpoints
    with xhr.JsonEntitiesFromCodec