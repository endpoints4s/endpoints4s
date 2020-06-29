package cqrs.webclient

import endpoints4s.xhr

object PublicEndpoints
    extends cqrs.publicserver.PublicEndpoints
    with xhr.faithful.Endpoints
    with xhr.JsonEntitiesFromCodecs
