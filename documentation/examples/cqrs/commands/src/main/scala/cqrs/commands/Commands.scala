package cqrs.commands


//#server
import endpoints.play.server.{Endpoints, JsonEntitiesFromCodec, PlayComponents}
import play.api.routing.Router

class Commands(protected val playComponents: PlayComponents) extends CommandsEndpoints with Endpoints with JsonEntitiesFromCodec {

  val routes: Router.Routes = routesFromEndpoints(

    command.implementedBy(CommandsService.apply),

    events.implementedBy(CommandsService.events)

  )

}
//#server