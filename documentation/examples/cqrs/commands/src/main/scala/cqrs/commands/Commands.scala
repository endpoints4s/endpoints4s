package cqrs.commands


//#server
import endpoints.play.server.{Endpoints, JsonEntitiesFromCodec}
import play.api.BuiltInComponents
import play.api.routing.Router

class Commands(protected val playComponents: BuiltInComponents) extends CommandsEndpoints with Endpoints with JsonEntitiesFromCodec {

  val routes: Router.Routes = routesFromEndpoints(

    command.implementedBy(CommandsService.apply),

    events.implementedBy(CommandsService.events)

  )

}
//#server