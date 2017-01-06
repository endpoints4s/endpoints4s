package cqrs

import endpoints.play.client.{CirceEntities, Endpoints}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

class QueriesService(wsClient: WSClient)(implicit ec: ExecutionContext)
  extends Endpoints("http://localhost:9000", wsClient)
    with CirceEntities
    with QueryEndpoints
