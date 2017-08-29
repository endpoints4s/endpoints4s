package cqrs.webclient.documented

import java.util.UUID

import endpoints.documented.delegate
import endpoints.xhr

object PublicEndpoints
  extends cqrs.publicserver.documented.PublicEndpoints
    with delegate.Endpoints
    with delegate.CirceJsonSchemaEntities
    with delegate.OptionalResponses {

  lazy val delegate =
    new xhr.faithful.Endpoints
      with xhr.CirceEntities
      with xhr.OptionalResponses

  implicit lazy val uuidSegment: Segment[UUID] =
    (uuid: UUID) => stringSegment.encode(uuid.toString)

}
