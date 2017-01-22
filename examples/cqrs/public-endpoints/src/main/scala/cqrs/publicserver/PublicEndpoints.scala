package cqrs.publicserver

import java.time.OffsetDateTime
import java.util.UUID

import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.queries.Meter
import endpoints.algebra.{CirceEntities, Endpoints, OptionalResponses}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.java8.time._

/**
  * Definition of the public HTTP API of our application.
  *
  * We expose a REST interface for manipulating meters.
  */
// TODO User authentication
// TODO All the commands should return the timestamp of the last performed event (so that clients can get consistent “write and read”)
trait PublicEndpoints extends Endpoints with CirceEntities with OptionalResponses {

  /** Common path prefix for endpoints: “/meters” */
  private val metersPath = path / "meters"

  /** Lists all the registered meters */
  val listMeters: Endpoint[Unit, List[Meter]] =
    endpoint(get(metersPath), jsonResponse[List[Meter]])

  /** Find a meter by id */
  val getMeter: Endpoint[UUID, Option[Meter]] =
    endpoint(get(metersPath / segment[UUID]), option(jsonResponse[Meter]))

  /** Registers a new meter */
  val createMeter: Endpoint[CreateMeter, Meter] =
    endpoint(post[Unit, CreateMeter, Unit, CreateMeter](metersPath, jsonRequest[CreateMeter]), jsonResponse[Meter])

  /** Add a record to an existing meter */
  val addRecord: Endpoint[(UUID, AddRecord), Unit] =
    endpoint(post[UUID, AddRecord, Unit, (UUID, AddRecord)](metersPath / segment[UUID] / "records", jsonRequest[AddRecord]), emptyResponse)

  implicit def uuidSegment: Segment[UUID]

}

object commands {

  case class CreateMeter(label: String)

  object CreateMeter {
    implicit val decoder: Decoder[CreateMeter] = deriveDecoder
    implicit val encoder: Encoder[CreateMeter] = deriveEncoder
  }

  case class AddRecord(date: OffsetDateTime, value: BigDecimal)

  object AddRecord {
    implicit val decoder: Decoder[AddRecord] = deriveDecoder
    implicit val encoder: Encoder[AddRecord] = deriveEncoder
  }

}