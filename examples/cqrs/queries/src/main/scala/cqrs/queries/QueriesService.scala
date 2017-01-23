package cqrs.queries

import java.util.UUID

import akka.actor.Scheduler
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import cqrs.commands.{CommandsEndpoints, MeterCreated, RecordAdded, StoredEvent}
import endpoints.play.client.{CirceEntities, Endpoints}

import scala.collection.immutable.SortedMap
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.concurrent.stm.{Ref, atomic}
/**
  * Implementation of the queries service
  */
class QueriesService(commandsBaseUrl: String, wsClient: WSClient, scheduler: Scheduler) {

  // --- public API

  def findById(id: UUID, maybeAfter: Option[Long]): Future[Option[Meter]] =
    updateIfRequired(maybeAfter)(_.meters.get(id))

  def findAll(): Future[List[Meter]] =
    Future.successful(stateRef.single.get.meters.values.to[List])

  // --- internals

  /** Client for the event store */
  private object store
    extends Endpoints(commandsBaseUrl, wsClient)
      with CirceEntities
      with CommandsEndpoints

  /** In-memory state */
  private val stateRef = Ref(
    State(
      lastEventTimestamp = None,
      meters = Map.empty
    )
  )

  // periodically poll the event store to keep our state up to date
  val _ = scheduler.schedule(0.seconds, 5.seconds) { update(); () }

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
  private def updateIfRequired[A](maybeTimestamp: Option[Long])(f: State => A): Future[A] = {
    val currentState = stateRef.single()
    maybeTimestamp.filter(t => currentState.lastEventTimestamp.forall(_ < t)) match {
      case None    => Future.successful(f(currentState))
      case Some(_) => update().map(f)
    }
  }

  /** Update the projection by fetching the last events from the event store and applying them to our state */
  private def update(): Future[State] =
    store.events(stateRef.single.get.lastEventTimestamp).map { (newEvents: Vector[StoredEvent]) =>
      atomic { implicit txn =>
        val currentState = stateRef()
        val newState =
          newEvents
            .dropWhile(e => currentState.lastEventTimestamp.exists(_ >= e.timestamp)) // Don’t apply events twice (in case several updates are performed in parallel)
            .foldLeft(currentState)(applyEvent)
        stateRef()= newState
        newState
      }
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
