package cqrs.commands

//#server
import cats.effect.IO
import endpoints4s.http4s.server.{Endpoints, JsonEntitiesFromCodecs}

class Commands extends Endpoints[IO] with JsonEntitiesFromCodecs with CommandsEndpoints {

  val routes = routesFromEndpoints(
    command.implementedBy(CommandsService.apply),
    events.implementedBy(CommandsService.events)
  )

}
//#server
