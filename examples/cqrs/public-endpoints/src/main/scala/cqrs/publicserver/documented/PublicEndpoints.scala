package cqrs.publicserver.documented

import java.util.UUID

import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import cqrs.queries.Meter

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
import endpoints.documented.algebra.{CirceEntities, Endpoints, OptionalResponses}

trait PublicEndpoints extends Endpoints with CirceEntities with OptionalResponses {

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

}
//#public-endpoints
