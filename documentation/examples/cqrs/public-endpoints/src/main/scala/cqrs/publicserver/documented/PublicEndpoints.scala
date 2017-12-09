package cqrs.publicserver.documented

import java.time.Instant
import java.util.UUID

import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.queries.Meter

import scala.collection.immutable.SortedMap

/**
  * Definition of the public HTTP API of our application using an
  * algebra that contains documentation information (thus we can
  * generate an OpenAPI definition of our API).
  *
  * Note: in a real application we would define ''either'' the `PublicEndpoints`
  * trait ''or'' the `DocumentedPublicEndpoints` trait, but not both. Here,
  * the goal is to show that there is little difference between both.
  */
//#public-endpoints
import endpoints.documented.algebra.{Endpoints, JsonSchemaEntities, OptionalResponses}

trait PublicEndpoints
  extends Endpoints
    with JsonSchemaEntities
    with endpoints.documented.generic.JsonSchemas
    with OptionalResponses {

  /** Common path prefix for endpoints: “/meters” */
  private val metersPath = path / "meters"
//#meter-id
  private val meterId = segment[UUID]("id")
//#meter-id

  /** Lists all the registered meters */
//#list-meters
  val listMeters: Endpoint[Unit, List[Meter]] =
    endpoint(
      get(metersPath),
      jsonResponse[List[Meter]](documentation = "All the meters")
    )
//#list-meters

  /** Find a meter by id */
//#meter-id
  val getMeter: Endpoint[UUID, Option[Meter]] =
    endpoint(
      get(metersPath / meterId),
      option(
        jsonResponse[Meter](documentation = "The meter identified by 'id'"),
        notFoundDocumentation = "Meter not found"
      )
    )
//#meter-id

  /** Registers a new meter */
  val createMeter/*: Endpoint[CreateMeter, Meter]*/ =
    endpoint(
      post(metersPath, jsonRequest[CreateMeter](documentation = Some("The meter to create"))),
      jsonResponse[Meter](documentation = "The created meter")
    )

  /** Add a record to an existing meter */
  val addRecord/*: Endpoint[(UUID, AddRecord), Meter]*/ =
    endpoint(
      post(metersPath / meterId / "records", jsonRequest[AddRecord](documentation = Some("The record to add"))),
      jsonResponse[Meter](documentation = "The updated meter")
    )

  implicit def uuidSegment: Segment[UUID]

  implicit lazy val instantJsonSchema: Record[Instant] =
    (
      field[Long]("seconds") zip
      field[Long]("nanos")
    ).invmap[Instant] { case (seconds, nanos) => Instant.ofEpochSecond(seconds, nanos) }(instant => (instant.getEpochSecond, instant.getNano.toLong))

  implicit lazy val uuidJsonSchema: JsonSchema[UUID] = stringJsonSchema.invmap(UUID.fromString)(_.toString) // TODO have a total `andThen` operator

  implicit lazy val timeSeriesJsonSchema: JsonSchema[SortedMap[Instant, BigDecimal]] = {
    implicit val entriesJsonSchema: Record[(Instant, BigDecimal)] = field[Instant]("key") zip field[BigDecimal]("value")
    arrayJsonSchema[List, (Instant, BigDecimal)]
      .invmap(entries => (SortedMap.newBuilder[Instant, BigDecimal] ++= entries).result())(_.to[List])
  }

  implicit lazy val meterJsonSchema: Record[Meter] =
    (
      field[UUID]("id") :×:
      field[String]("label") :×:
      field[SortedMap[Instant, BigDecimal]]("timeSeries")
    ).as[Meter]

  implicit lazy val createMeterJsonSchema: JsonSchema[CreateMeter] = genericJsonSchema[CreateMeter]

  implicit lazy val addRecordJsonSchema: JsonSchema[AddRecord] = genericJsonSchema[AddRecord]

}
//#public-endpoints
