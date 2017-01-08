package cqrs

import java.time.OffsetDateTime
import java.util.UUID

/** A meter has a time series of reading */
// TODO store the number of digits of the meter
case class Meter(id: UUID, timeSeries: List[Reading])

/** A reading is a value at a certain date */
case class Reading(date: OffsetDateTime, value: BigDecimal)

object Meter {

  /**
    * Defines the behavior of a `Meter` as an event produced by the application
    * of a command to an aggregate.
    *
    * @param maybeAggregate Whether The aggregate the command is applied to. Or
    *                       `None` if the command is not applied to an existing
    *                       aggregate
    * @param command        The command to apply
    * @return The produced event, or `None` if the command was not valid.
    */
  // TODO Return meaningful information in case of invalid command
  def handleCommand(maybeAggregate: Option[Meter], command: Command): Option[Event] =
    maybeAggregate match {
      case None =>
        command match {
          case CreateMeter => Some(MeterCreated(UUID.randomUUID()))
          case _ => None
        }
      case Some(meter) =>
        command match {
          case AddReading(date, value) => Some(ReadingAdded(date, value))
          case _ => None
        }
    }

  def handleEvent(maybeMeter: Option[Meter], event: Event): Meter =
    (maybeMeter, event) match {
      case (None, MeterCreated(id)) => Meter(id, Nil)
      case (Some(meter), ReadingAdded(date, value)) =>
        meter.copy(timeSeries = Reading(date, value) :: meter.timeSeries)
      case e => sys.error(s"No event handlers defined for $e")
    }

}