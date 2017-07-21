package cqrs.publicserver

import endpoints.play.client.{CirceEntities, Endpoints}
import cqrs.queries.QueriesEndpoints
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

class QueriesClient(baseUrl: String, wsClient: WSClient)(implicit ec: ExecutionContext)
  extends Endpoints(baseUrl, wsClient)
    with CirceEntities
    with QueriesEndpoints
