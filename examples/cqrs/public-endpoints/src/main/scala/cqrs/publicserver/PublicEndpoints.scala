package cqrs.publicserver

import java.time.OffsetDateTime
import java.util.UUID

import commands.{AddRecord, CreateMeter}
import cqrs.queries.Meter
import endpoints.algebra.{CirceEntities, Endpoints, OptionalResponses}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.java8.time._

trait PublicEndpoints extends Endpoints with CirceEntities with OptionalResponses {

  private val pathPrefix = path / "meters"

  val createMeter: Endpoint[CreateMeter, Option[Meter]] =
    endpoint(post[Unit, CreateMeter, Unit, CreateMeter](pathPrefix, jsonRequest[CreateMeter]), option(jsonResponse[Meter]))

  val addRecord: Endpoint[AddRecord, Unit] =
    endpoint(post[Unit, AddRecord, Unit, AddRecord](pathPrefix, jsonRequest[AddRecord]), emptyResponse)

  val listMeters: Endpoint[Unit, List[Meter]] =
    endpoint(get(pathPrefix), jsonResponse[List[Meter]])

}

object commands {

  case class CreateMeter(label: String)

  object CreateMeter {
    implicit val decoder: Decoder[CreateMeter] = deriveDecoder
    implicit val encoder: Encoder[CreateMeter] = deriveEncoder
  }

  case class AddRecord(id: UUID, date: OffsetDateTime, value: BigDecimal)

  object AddRecord {
    implicit val decoder: Decoder[AddRecord] = deriveDecoder
    implicit val encoder: Encoder[AddRecord] = deriveEncoder
  }

}