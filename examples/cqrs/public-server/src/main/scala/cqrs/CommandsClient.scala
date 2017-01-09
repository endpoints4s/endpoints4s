package cqrs

import endpoints.play.client.{CirceEntities, Endpoints}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

class CommandsClient(baseUrl: String, wsClient: WSClient)(implicit ec: ExecutionContext)
  extends Endpoints(baseUrl, wsClient)
    with CirceEntities
    with CommandsEndpoints
