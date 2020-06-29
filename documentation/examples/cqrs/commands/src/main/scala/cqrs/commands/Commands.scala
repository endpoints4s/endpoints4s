package cqrs.commands

//#server
import endpoints4s.play.server.{Endpoints, JsonEntitiesFromCodecs, PlayComponents}
import play.api.routing.Router

class Commands(val playComponents: PlayComponents)
    extends CommandsEndpoints
    with Endpoints
    with JsonEntitiesFromCodecs {

  val routes: Router.Routes = routesFromEndpoints(
    command.implementedBy(CommandsService.apply),
    events.implementedBy(CommandsService.events)
  )

}
//#server
