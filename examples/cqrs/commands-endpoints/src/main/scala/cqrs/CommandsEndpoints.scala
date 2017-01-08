package cqrs

import java.time.OffsetDateTime
import java.util.UUID

import endpoints.algebra.{CirceEntities, Endpoints}
import io.circe.{Decoder, Encoder}
import io.circe.java8.time._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

trait CommandsEndpoints extends Endpoints with CirceEntities {

  val command: Endpoint[CommandReq, CommandResp] =
    endpoint(post[Unit, CommandReq, Unit, CommandReq](path / "command", jsonRequest[CommandReq]), jsonResponse[CommandResp])

}

/** A request applying a command on the given aggregate */
case class CommandReq(
  maybeAggregateId: Option[UUID],
  command: Command
)

object CommandReq {
  implicit val decoder: Decoder[CommandReq] = deriveDecoder
  implicit val encoder: Encoder[CommandReq] = deriveEncoder

  // Convenient constructors
  val createMeter: CommandReq = CommandReq(None, CreateMeter)
  def addReading(id: UUID, date: OffsetDateTime, value: BigDecimal): CommandReq =
    CommandReq(Some(id), AddReading(date, value))
}

/**
  * The response resulting from the command application
  *
  * @param maybeEvent The produced event, or `None` in case of failure (aggregate
  *                   not found or invalid command)
  */
case class CommandResp(maybeEvent: Option[Event])

object CommandResp {
  implicit val decoder: Decoder[CommandResp] = deriveDecoder
  implicit val encoder: Encoder[CommandResp] = deriveEncoder
}

sealed trait Command
case object CreateMeter extends Command
case class AddReading(date: OffsetDateTime, value: BigDecimal) extends Command

object Command {
  implicit val decoder: Decoder[Command] = deriveDecoder
  implicit val encoder: Encoder[Command] = deriveEncoder
}

sealed trait Event
case class MeterCreated(id: UUID) extends Event
case class ReadingAdded(date: OffsetDateTime, value: BigDecimal) extends Event

object Event {
  implicit val decoder: Decoder[Event] = deriveDecoder
  implicit val encoder: Encoder[Event] = deriveEncoder
}