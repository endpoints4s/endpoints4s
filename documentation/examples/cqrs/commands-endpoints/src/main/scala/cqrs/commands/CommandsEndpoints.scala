package cqrs.commands

import java.time.Instant
import java.util.UUID

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

//#endpoints
import endpoints4s.algebra.Endpoints
import endpoints4s.algebra.circe.JsonEntitiesFromCodecs

trait CommandsEndpoints extends Endpoints with JsonEntitiesFromCodecs {

//#microservice-endpoint-description
  /** Application of a command.
    *
    * Returns the produced event, or `None` in case of failure (aggregate
    * not found or invalid command).
    */
  val command: Endpoint[Command, Option[StoredEvent]] =
    endpoint(
      post(path / "command", jsonRequest[Command]),
      ok(jsonResponse[Option[StoredEvent]])
    )
//#microservice-endpoint-description

  /** Read the event long (optionally from a given timestamp).
    */
  val events: Endpoint[Option[Long], Seq[StoredEvent]] =
    endpoint(
      get(path / "events" /? qs[Option[Long]]("since")),
      ok(jsonResponse[Seq[StoredEvent]])
    )

}
//#endpoints

/** Base trait of commands.
  *
  * Note that we could also have just reused the DTOs of the public API,
  * but we chose to use distinct data types so that the public API is
  * not cluttered with implementation details of the commands microservice.
  */
sealed trait Command

/** Base trait of commands creating a new resource */
sealed trait CreationCommand extends Command

/** Base trait of commands updating an existing resource */
sealed trait UpdateCommand extends Command {
  def meterId: UUID
}

/** Create a new meter */
case class CreateMeter(label: String) extends CreationCommand

/** Add a record for an existing meter */
case class AddRecord(meterId: UUID, date: Instant, value: BigDecimal) extends UpdateCommand

object Command {
  implicit val decoder: Decoder[Command] = deriveDecoder
  implicit val encoder: Encoder[Command] = deriveEncoder
}

case class StoredEvent(timestamp: Long, event: Event)

object StoredEvent {
  implicit val decoder: Decoder[StoredEvent] = deriveDecoder
  implicit val encoder: Encoder[StoredEvent] = deriveEncoder
}

sealed trait Event
case class MeterCreated(id: UUID, label: String) extends Event
case class RecordAdded(id: UUID, date: Instant, value: BigDecimal) extends Event

object Event {
  implicit val decoder: Decoder[Event] = deriveDecoder
  implicit val encoder: Encoder[Event] = deriveEncoder
}
