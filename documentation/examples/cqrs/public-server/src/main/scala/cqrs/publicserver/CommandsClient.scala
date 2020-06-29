package cqrs.publicserver

import endpoints4s.play.client.{JsonEntitiesFromCodecs, Endpoints}
import cqrs.commands.CommandsEndpoints
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

class CommandsClient(baseUrl: String, wsClient: WSClient)(implicit
    ec: ExecutionContext
) extends Endpoints(baseUrl, wsClient)
    with JsonEntitiesFromCodecs
    with CommandsEndpoints
