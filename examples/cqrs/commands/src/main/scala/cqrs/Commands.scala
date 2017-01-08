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
    command.implementedBy { commandReq =>
      CommandResp(
        processCommand(commandReq.maybeAggregateId, commandReq.command)
      )
    }
  )

  val aggregatesRef = Ref(Map.empty[UUID, Meter])

  def processCommand(maybeAggregateId: Option[UUID], command: Command): Option[Event] =
    atomic { implicit txn =>

      def applyToAggregate(maybeAggregate: Option[Meter], command: Command): Option[Event] = {
        val maybeEvent = Meter.handleCommand(maybeAggregate, command)

        maybeEvent.foreach { event =>
          val aggregate = Meter.handleEvent(maybeAggregate, event)
          aggregatesRef()= aggregatesRef.get + (aggregate.id -> aggregate)
        }

        maybeEvent
      }

      maybeAggregateId match {
        case Some(aggregateId) =>
          aggregatesRef.get.get(aggregateId)
            .flatMap(aggregate => applyToAggregate(Some(aggregate), command))
        case None => applyToAggregate(None, command)
      }
    }

}
