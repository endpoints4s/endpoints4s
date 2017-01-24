package cqrs.commands


//#server
import endpoints.play.routing.{CirceEntities, Endpoints}
import play.api.routing.Router

object Commands extends CommandsEndpoints with Endpoints with CirceEntities {

  val routes: Router.Routes = routesFromEndpoints(

    command.implementedBy(CommandsService.apply),

    events.implementedBy(CommandsService.events)

  )

}
//#server