package cqrs.webclient

import java.util.UUID

import endpoints.xhr

object PublicEndpoints
  extends cqrs.publicserver.PublicEndpoints
    with xhr.faithful.Endpoints
    with xhr.CirceEntities
    with xhr.OptionalResponses {

  implicit lazy val uuidSegment: Segment[UUID] = (uuid: UUID) => uuid.toString

}