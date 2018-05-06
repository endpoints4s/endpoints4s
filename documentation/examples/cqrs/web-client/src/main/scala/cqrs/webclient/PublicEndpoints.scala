package cqrs.webclient

import java.util.UUID

import endpoints.xhr

object PublicEndpoints
  extends cqrs.publicserver.PublicEndpoints
    with xhr.faithful.Endpoints
    with xhr.JsonEntitiesFromCodec {

  //#segment-uuid
  implicit lazy val uuidSegment: Segment[UUID] =
    (uuid: UUID) => stringSegment.encode(uuid.toString)
  //#segment-uuid

}