package cqrs.commands

import java.time.Instant
import java.util.UUID

/** This is the model used to validate commands.
  * It is not necessary the same as the one for querying.
  *
  * For instance, here we use a `List` to store the records,
  * because the ordering does not matter in the context of the
  * application of a command. Conversely, the query model
  * represents records as a `SortedMap` because it is useful
  * to see the time series in chronological order.
  */
// TODO store the number of digits of the meter
case class Meter(id: UUID, timeSeries: List[Record])

/** A reading is a value at a certain date */
case class Record(date: Instant, value: BigDecimal)

object Meter {

  /** @return The produced event, or `None` if the command was not valid.
    */
  // TODO Return meaningful information in case of invalid command
  def handleCreationCommand(creationCommand: CreationCommand): Option[Event] =
    creationCommand match {
      case CreateMeter(label) => Some(MeterCreated(UUID.randomUUID(), label))
    }

  def handleUpdateCommand(
      meter: Meter,
      updateCommand: UpdateCommand
  ): Option[Event] =
    updateCommand match {
      case AddRecord(id, date, value) => Some(RecordAdded(id, date, value))
    }

  def handleEvent(maybeMeter: Option[Meter], event: Event): Meter =
    (maybeMeter, event) match {
      case (None, MeterCreated(id, _)) => Meter(id, Nil)
      case (Some(meter), RecordAdded(_, date, value)) =>
        meter.copy(timeSeries = Record(date, value) :: meter.timeSeries)
      case e => sys.error(s"No event handlers defined for $e")
    }

}
