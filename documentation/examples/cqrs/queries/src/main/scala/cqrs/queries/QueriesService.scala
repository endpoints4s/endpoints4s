package cqrs.queries

import cats.effect.IO

import java.util.UUID
import org.http4s.client.{Client => Http4sClient}

import cqrs.commands.{CommandsEndpoints, MeterCreated, RecordAdded, StoredEvent}
import org.http4s.Uri

import scala.collection.immutable.SortedMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.stm.{Ref, atomic}

/** Implementation of the queries service
  */
class QueriesService(
    commandsAuthority: Uri.Authority,
    http4sClient: Http4sClient[IO]
) {

  // --- public API

  def findById(id: UUID, maybeAfter: Option[Long]): IO[Option[Meter]] =
    updateIfRequired(maybeAfter)(_.meters.get(id))

  def findAll(): IO[List[Meter]] =
    IO.pure(stateRef.single.get.meters.values.toList)

  // --- internals

  //#event-log-client
  import endpoints4s.http4s.client

  /** Client for the event log */
  private object eventLog
      extends client.Endpoints(commandsAuthority, Uri.Scheme.http, http4sClient)
      with client.JsonEntitiesFromCodecs
      with CommandsEndpoints
  //#event-log-client

  /** In-memory state */
  private val stateRef = Ref(
    State(
      lastEventTimestamp = None,
      meters = Map.empty
    )
  )

  // periodically poll the event log to keep our state up to date
  def pollEventLog: IO[Unit] =
    for {
      _ <- update()
      _ <- IO.sleep(5.seconds)
      _ <- pollEventLog
    } yield ()
  pollEventLog.background.use(_ => IO.never)

  /** Internal state */
  case class State(
      lastEventTimestamp: Option[Long],
      meters: Map[UUID, Meter]
  )

  /** Update the internal state so that the timestamp of the last applied event is greater or
    * equal to the given timestamp.
    *
    * This is used by the public service to get consistent write-and-read.
    */
  private def updateIfRequired[A](
      maybeTimestamp: Option[Long]
  )(f: State => A): IO[A] = {
    val currentState = stateRef.single()
    maybeTimestamp.filter(t => currentState.lastEventTimestamp.forall(_ < t)) match {
      case None    => IO.pure(f(currentState))
      case Some(_) => update().map(f)
    }
  }

  /** Update the projection by fetching the last events from the event store and applying them to our state */
  private def update(): IO[State] = {

    val maybeLastEventTimestamp = stateRef.single.get.lastEventTimestamp

    def atomicallyApplyEvents(events: Seq[StoredEvent]): State =
      atomic { implicit txn =>
        val currentState = stateRef()
        val newState =
          events
            .dropWhile(e =>
              currentState.lastEventTimestamp.exists(_ >= e.timestamp)
            ) // Don’t apply events twice (in case several updates are performed in parallel)
            .foldLeft(currentState)(applyEvent)
        stateRef() = newState
        newState
      }

    //#invocation
    val eventuallyUpdatedState: IO[State] =
      eventLog.events
        .sendAndConsume(maybeLastEventTimestamp)
        .map { (newEvents: Seq[StoredEvent]) =>
          atomicallyApplyEvents(newEvents)
        }
    //#invocation
    eventuallyUpdatedState
  }

  /** Apply an event to a given state */
  private def applyEvent(state: State, storedEvent: StoredEvent): State =
    storedEvent match {
      case StoredEvent(t, MeterCreated(id, label)) =>
        state.copy(
          lastEventTimestamp = Some(t),
          meters = state.meters + (id -> Meter(id, label, SortedMap.empty))
        )
      case StoredEvent(t, RecordAdded(id, date, value)) =>
        val meter = state.meters(id)
        val updatedMeter =
          meter.copy(timeSeries = meter.timeSeries + (date -> value))
        state.copy(
          lastEventTimestamp = Some(t),
          meters = state.meters + (id -> updatedMeter)
        )
    }

}
