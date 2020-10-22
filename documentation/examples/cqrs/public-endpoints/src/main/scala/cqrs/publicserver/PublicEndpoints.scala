package cqrs.publicserver

import java.time.Instant
import java.util.UUID

import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.queries.Meter
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/** Definition of the public HTTP API of our application.
  *
  * We expose a REST interface for manipulating meters.
  */
// TODO User authentication
//#public-endpoints
import endpoints4s.algebra.{circe, Endpoints}

trait PublicEndpoints extends Endpoints with circe.JsonEntitiesFromCodecs {

  /** Common path prefix for endpoints: “/meters” */
  private val metersPath = path / "meters"

  /** Lists all the registered meters */
  val listMeters: Endpoint[Unit, List[Meter]] =
    endpoint(get(metersPath), ok(jsonResponse[List[Meter]]))

  /** Find a meter by id */
//#get-meter
  val getMeter: Endpoint[UUID, Option[Meter]] =
    endpoint(
      get(metersPath / segment[UUID]()),
      ok(jsonResponse[Meter]).orNotFound()
    )
//#get-meter

  //#webapps-endpoint
  /** Registers a new meter */
  val createMeter: Endpoint[CreateMeter, Meter] =
    endpoint(
      post(metersPath, jsonRequest[CreateMeter]),
      ok(jsonResponse[Meter])
    )
  //#webapps-endpoint

  /** Add a record to an existing meter */
  val addRecord: Endpoint[(UUID, AddRecord), Meter] =
    endpoint(
      post(metersPath / segment[UUID]() / "records", jsonRequest[AddRecord]),
      ok(jsonResponse[Meter])
    )

}
//#public-endpoints

object commands {

  case class CreateMeter(label: String)

  object CreateMeter {
    implicit val decoder: Decoder[CreateMeter] = deriveDecoder
    implicit val encoder: Encoder[CreateMeter] = deriveEncoder
  }

  case class AddRecord(date: Instant, value: BigDecimal)

  object AddRecord {
    implicit val decoder: Decoder[AddRecord] = deriveDecoder
    implicit val encoder: Encoder[AddRecord] = deriveEncoder
  }

}
