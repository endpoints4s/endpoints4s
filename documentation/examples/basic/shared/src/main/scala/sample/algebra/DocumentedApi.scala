package sample.algebra

import endpoints4s.algebra.{AuthenticatedEndpoints, circe, Endpoints}
import io.circe.generic.JsonCodec

trait DocumentedApi
    extends Endpoints
    with AuthenticatedEndpoints
    with circe.JsonEntitiesFromCodecs {

  val items =
    endpoint(
      get(
        path / "items" / segment[String]("category") /? qs[Option[Int]]("page")
      ),
      ok(
        jsonResponse[List[Item]],
        Some("List all the items of the given category")
      )
    )

  val itemId = segment[String]("id")

  val item =
    endpoint(
      get(path / "item" / itemId),
      wheneverFound(
        ok(jsonResponse[Item], Some("The item identified by 'id'")),
        Some("Item not found")
      )
    )

  val admin =
    endpoint(
      get(path / "admin"),
      ok(emptyResponse, Some("Administration page")),
      docs = EndpointDocs().withSummary(Some("Authentication endpoint"))
    ).withBasicAuth()

}

@JsonCodec
case class Item(name: String)
