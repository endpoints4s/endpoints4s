package cqrs

import java.util.UUID

import endpoints.play.routing.{CirceEntities, Endpoints}
import play.api.routing.Router

import scala.concurrent.stm.{Ref, atomic}

/**
  * Implementation of the commands service.
  *
  * We use an in-memory storage for simplicity but we could easily, for instance, have
  * one cassandra node per aggregate.
  */
object Commands extends CommandsEndpoints with Endpoints with CirceEntities {

  val routes: Router.Routes = routesFromEndpoints(
    command.implementedBy(processCommand)
  )

  val aggregatesRef = Ref(Map.empty[UUID, Meter])

  def processCommand(command: Command): Option[Event] =
    atomic { implicit txn =>

      def handleEvent(maybeEvent: Option[Event], maybeAggregate: Option[Meter]): Option[Event] = {
        maybeEvent.foreach { event =>
          val aggregate = Meter.handleEvent(maybeAggregate, event)
          // TODO Event log
          aggregatesRef()= aggregatesRef.get + (aggregate.id -> aggregate)
        }
        maybeEvent
      }

      command match {
        case creation: CreationCommand =>
          handleEvent(Meter.handleCreationCommand(creation), None)
        case update: UpdateCommand =>
          aggregatesRef.get.get(update.meterId).flatMap { aggregate =>
            handleEvent(Meter.handleUpdateCommand(aggregate, update), Some(aggregate))
          }
      }
    }

}
