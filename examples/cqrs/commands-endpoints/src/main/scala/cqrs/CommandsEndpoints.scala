package cqrs

import java.time.OffsetDateTime
import java.util.UUID

import endpoints.algebra.{CirceEntities, Endpoints}
import io.circe.{Decoder, Encoder}
import io.circe.java8.time._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

trait CommandsEndpoints extends Endpoints with CirceEntities {

  /**
    * Application of a command.
    *
    * Returns the produced event, or `None` in case of failure (aggregate
    * not found or invalid command).
    */
  val command: Endpoint[Command, Option[Event]] =
    endpoint(post[Unit, Command, Unit, Command](path / "command", jsonRequest[Command]), jsonResponse[Option[Event]])

}

sealed trait Command

/** Base trait of commands creating a new resource */
sealed trait CreationCommand extends Command

/** Base trait of commands updating an existing resource */
sealed trait UpdateCommand extends Command {
  def meterId: UUID
}

/** Create a new meter */
case object CreateMeter extends CreationCommand

/** Add a record for an existing meter */
case class AddRecord(meterId: UUID, date: OffsetDateTime, value: BigDecimal) extends UpdateCommand

object Command {
  implicit val decoder: Decoder[Command] = deriveDecoder
  implicit val encoder: Encoder[Command] = deriveEncoder
}

sealed trait Event
case class MeterCreated(id: UUID) extends Event
case class RecordAdded(date: OffsetDateTime, value: BigDecimal) extends Event

object Event {
  implicit val decoder: Decoder[Event] = deriveDecoder
  implicit val encoder: Encoder[Event] = deriveEncoder
}
