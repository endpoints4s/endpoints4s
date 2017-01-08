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
    * @return The produced event, or `None` if the command was not valid.
    */
  // TODO Return meaningful information in case of invalid command
  def handleCreationCommand(creationCommand: CreationCommand): Option[Event] =
    creationCommand match {
      case CreateMeter => Some(MeterCreated(UUID.randomUUID()))
    }

  def handleUpdateCommand(meter: Meter, updateCommand: UpdateCommand): Option[Event] =
    updateCommand match {
      case AddRecord(_, date, value) => Some(RecordAdded(date, value))
    }

  def handleEvent(maybeMeter: Option[Meter], event: Event): Meter =
    (maybeMeter, event) match {
      case (None, MeterCreated(id)) => Meter(id, Nil)
      case (Some(meter), RecordAdded(date, value)) =>
        meter.copy(timeSeries = Reading(date, value) :: meter.timeSeries)
      case e => sys.error(s"No event handlers defined for $e")
    }

}