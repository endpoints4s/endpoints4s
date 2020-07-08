package cqrs.publicserver

import endpoints4s.play.client.{JsonEntitiesFromCodecs, Endpoints, MuxEndpoints}
import cqrs.queries.QueriesEndpoints
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

class QueriesClient(baseUrl: String, wsClient: WSClient)(implicit
    ec: ExecutionContext
) extends Endpoints(baseUrl, wsClient)
    with JsonEntitiesFromCodecs
    with MuxEndpoints
    with QueriesEndpoints
