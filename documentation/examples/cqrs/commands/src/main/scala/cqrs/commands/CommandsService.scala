package cqrs.commands

import java.util.UUID

import scala.concurrent.stm.{Ref, atomic}

/** Implementation of the commands service.
  *
  * We use an in-memory storage for simplicity but we could easily, for instance, have
  * one cassandra node per aggregate.
  */
object CommandsService {

  /** Aggregates’ states and event log */
  private val aggregatesRef = Ref(State(Map.empty, Vector.empty, 0))

  // TODO more useful failure data
  //#signatures
  /** Atomically applies a command to the current aggregates.
    * @return The completed event, or `None` if the command was not applicable
    */
  def apply(command: Command): Option[StoredEvent] = // …
    //#signatures
    atomic { implicit txn =>
      val state = aggregatesRef()

      def handleEvent(
          maybeEvent: Option[Event],
          maybeAggregate: Option[Meter]
      ): Option[StoredEvent] =
        maybeEvent.map { event =>
          val aggregate = Meter.handleEvent(maybeAggregate, event)
          val timestamp = state.lastTimestamp + 1
          val storedEvent = StoredEvent(timestamp, event)
          aggregatesRef() = state.copy(
            data = state.data + (aggregate.id -> aggregate),
            eventLog = state.eventLog :+ storedEvent,
            timestamp
          )
          storedEvent
        }

      command match {
        case creation: CreationCommand =>
          handleEvent(Meter.handleCreationCommand(creation), None)
        case update: UpdateCommand =>
          state.data.get(update.meterId).flatMap { aggregate =>
            handleEvent(
              Meter.handleUpdateCommand(aggregate, update),
              Some(aggregate)
            )
          }
      }

    }

  /** Internal state of the commands service */
  case class State(
      data: Map[UUID, Meter],
      eventLog: Vector[StoredEvent],
      lastTimestamp: Long
  )

  //#signatures
  /** @return The sequence of events stored in the log. Events that happened
    *         before the given optional timestamp are discarded.
    */
  def events(maybeSince: Option[Long]): Vector[StoredEvent] = // …
    //#signatures
    aggregatesRef
      .single()
      .eventLog
      .dropWhile(e => maybeSince.exists(e.timestamp < _))

}
