package cqrs.commands

import endpoints.play.routing.{CirceEntities, Endpoints}
import play.api.routing.Router

/**
  * Implementation of the commands service.
  */
object Commands extends CommandsEndpoints with Endpoints with CirceEntities {

  val routes: Router.Routes = routesFromEndpoints(

    command.implementedBy(CommandHandler.apply),

    events.implementedBy(CommandHandler.events)

  )

}
